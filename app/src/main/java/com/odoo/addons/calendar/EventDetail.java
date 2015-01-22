/**
 * Odoo, Open Source Management Solution
 * Copyright (C) 2012-today Odoo SA (<http:www.odoo.com>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http:www.gnu.org/licenses/>
 *
 * Created on 9/1/15 5:46 PM
 */
package com.odoo.addons.calendar;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.odoo.addons.calendar.models.CalendarEvent;
import com.odoo.addons.calendar.utils.CalendarUtils;
import com.odoo.addons.calendar.utils.EventColorDialog;
import com.odoo.addons.calendar.utils.ReminderDialog;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OValues;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.utils.OActionBarUtils;
import com.odoo.core.utils.OAlert;
import com.odoo.core.utils.OControls;
import com.odoo.core.utils.ODateUtils;
import com.odoo.core.utils.OResource;
import com.odoo.core.utils.reminder.ReminderUtils;
import com.odoo.crm.R;

import java.util.Date;

import odoo.controls.OField;
import odoo.controls.OForm;

public class EventDetail extends ActionBarActivity implements View.OnClickListener,
        EventColorDialog.OnColorSelectListener, OField.IOnFieldValueChangeListener, ReminderDialog.OnReminderValueSelectListener {
    public static final String TAG = EventDetail.class.getSimpleName();
    private ActionBar actionBar;
    private static final String KEY_EXTRA_EVENT_COLOR = "event_color";
    private static final String KEY_COLOR_DATA = "color_data";
    private String mEventColor = CalendarUtils.getBackgroundColors()[0];
    private OForm eventForm;
    private Integer mEventColorCode = 0;
    private ReminderDialog.ReminderItem mReminder;
    private ODataRow color_data = null;
    private Boolean mAllDay = false;
    private View mView = null;
    private CalendarEvent calendarEvent;
    private int row_id = -1;
    private OField event_date_end, event_date_start, event_time_end, event_time_start, allDay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calendar_event_detail_form);
        OActionBarUtils.setActionBar(this, true);
        calendarEvent = new CalendarEvent(this, null);
        actionBar = getSupportActionBar();
        actionBar.setHomeAsUpIndicator(R.drawable.ic_action_mark_undone);
        actionBar.setTitle("New Meeting");
        mView = findViewById(R.id.eventForm);
        event_date_end = (OField) findViewById(R.id.event_date_end);
        event_time_end = (OField) findViewById(R.id.event_end_time);
        event_date_start = (OField) findViewById(R.id.event_date_start);
        event_time_start = (OField) findViewById(R.id.event_start_time);
        allDay = (OField) findViewById(R.id.fieldAllDay);
        if (savedInstanceState != null) {
            mEventColor = savedInstanceState.getString(KEY_EXTRA_EVENT_COLOR);
            color_data = savedInstanceState.getParcelable(KEY_COLOR_DATA);
            colorSelected(color_data);
        } else {
            setThemeColor(mEventColor);
        }
        allDay.setOnValueChangeListener(this);
        ((OField) findViewById(R.id.event_date_start)).setOnValueChangeListener(this);
        ((OField) findViewById(R.id.event_start_time)).setOnValueChangeListener(this);
        // OnClicks
        findViewById(R.id.event_color).setOnClickListener(this);
        findViewById(R.id.reminderForEvent).setOnClickListener(this);

        eventForm = (OForm) mView;
        if (getIntent().getExtras() != null && getIntent().getExtras().containsKey(OColumn.ROW_ID)) {
            row_id = getIntent().getIntExtra(OColumn.ROW_ID, -1);
            if (row_id != -1) {
                actionBar.setTitle("Edit Meeting");
                ODataRow record = calendarEvent.browse(row_id);
                eventForm.initForm(record);
                allDay.setValue(record.getBoolean("allday"));
                String dateFormat = (record.getBoolean("allday")) ? ODateUtils.DEFAULT_DATE_FORMAT :
                        ODateUtils.DEFAULT_FORMAT;
                event_date_start.setValue(ODateUtils.parseDate(record.getString("date_start"), dateFormat,
                        ODateUtils.DEFAULT_DATE_FORMAT));
                event_date_end.setValue(ODateUtils.parseDate(record.getString("date_end"), dateFormat,
                        ODateUtils.DEFAULT_DATE_FORMAT));
                event_time_start.setValue(ODateUtils.parseDate(record.getString("date_start"), dateFormat,
                        ODateUtils.DEFAULT_TIME_FORMAT));
                event_time_end.setValue(ODateUtils.parseDate(record.getString("date_end"), dateFormat,
                        ODateUtils.DEFAULT_TIME_FORMAT));
                colorSelected(CalendarUtils.getColorData(record.getInt("color_index")));
            } else {
                eventForm.initForm(null);
            }
        } else {
            eventForm.initForm(null);
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.event_color:
                CalendarUtils.colorDialog(this, mEventColor, this).show();
                break;
            case R.id.reminderForEvent:
                ReminderDialog dialog = new ReminderDialog(this,
                        (mAllDay) ? ReminderDialog.ReminderType.FullDayEvent :
                                ReminderDialog.ReminderType.TimeBasedEvent);
                dialog.setOnReminderValueSelectListener(this);
                dialog.show();
                break;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_EXTRA_EVENT_COLOR, mEventColor);
        outState.putParcelable(KEY_COLOR_DATA, color_data);
    }

    @Override
    public void colorSelected(ODataRow color_data) {
        if (color_data != null) {
            mEventColor = color_data.getString("code");
            this.color_data = color_data;
            ImageView event_color_view = (ImageView) findViewById(R.id.event_color_view);
            event_color_view.setColorFilter(Color.parseColor(mEventColor));
            OControls.setText(mView, R.id.event_color_label,
                    color_data.getString("label"));
            mEventColorCode = color_data.getInt("index");
            setThemeColor(mEventColor);
        }
    }

    private void setThemeColor(String color_code) {
        int color = Color.parseColor(color_code);
        actionBar.setBackgroundDrawable(new ColorDrawable(color));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_calendar_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.menu_calendar_detail_save:
                OValues values = eventForm.getValues();
                if (values != null) {
                    createMeeting(values);
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void createMeeting(OValues values) {
        OValues meeting = new OValues();
        meeting.put("name", values.get("name"));
        meeting.put("allday", values.get("allday"));
        meeting.put("location", values.get("location"));
        meeting.put("description", values.get("description"));
        meeting.put("color_index", mEventColorCode);
        if (calendarEvent.getColumn("date") == null) {
            //v7+
            if (values.getBoolean("allday")) {
                meeting.put("start_date", values.get("event_date_start"));
                meeting.put("stop_date", values.get("event_date_end"));
                meeting.put("date_start", meeting.get("start_date"));
                meeting.put("date_end", meeting.get("stop_date"));
            } else {
                String start_datetime = values.get("event_date_start") + " " + values.get("event_time_start");
                String stop_datetime = values.get("event_date_end") + " " + values.get("event_time_end");
                meeting.put("start_datetime", start_datetime);
                meeting.put("stop_datetime", stop_datetime);
                meeting.put("date_start", meeting.get("start_datetime"));
                meeting.put("date_end", meeting.get("stop_datetime"));
            }
        } else {
            //v7
            String start_datetime = values.get("event_date_start") + " " + values.get("event_time_start");
            String stop_datetime = values.get("event_date_end") + " " + values.get("event_time_end");
            meeting.put("date", start_datetime);
            meeting.put("date_deadline", stop_datetime);
            meeting.put("date_start", meeting.get("date"));
            meeting.put("date_end", meeting.get("date_deadline"));
        }

        String format = (meeting.getBoolean("allday")) ? ODateUtils.DEFAULT_DATE_FORMAT :
                ODateUtils.DEFAULT_FORMAT;
        Date date_start = ODateUtils.createDateObject(meeting.getString("date_start"),
                format, false);
        Date date_end = ODateUtils.createDateObject(meeting.getString("date_end"),
                format, false);
        if (date_end.compareTo(date_start) < 0) {
            OAlert.showWarning(this, OResource.string(this, R.string.error_end_date_small_than_start_date));
        } else {
            Date now = new Date();
            Date reminderDate = null;
            if (now.compareTo(date_start) < 0) {
                meeting.put("has_reminder", "true");
                if (mReminder == null) {
                    mReminder = ReminderDialog.getDefault(this, meeting.getBoolean("allday"));
                }
                reminderDate = ReminderDialog.getReminderDateTime(meeting.getString("date_start"),
                        meeting.getBoolean("allday"), mReminder);
                if (reminderDate != null) {
                    meeting.put("reminder_datetime", ODateUtils.getDate(reminderDate, ODateUtils.DEFAULT_FORMAT));
                }
            }
            if (row_id != -1) {
                Log.i(TAG, "Event updated");
                calendarEvent.update(row_id, meeting);
            } else {
                Log.i(TAG, "Event created");
                row_id = calendarEvent.insert(meeting);
            }
            Bundle extra = new Bundle();
            extra.putInt(OColumn.ROW_ID, row_id);
            extra.putString(ReminderUtils.KEY_REMINDER_TYPE, "event");
            if (reminderDate != null) {
                if (ReminderUtils.get(getApplicationContext()).setReminder(reminderDate, extra)) {
                    Log.i(TAG, "Reminder added.");
                }
            }
            finish();
        }
    }

    public void onCheckedChanged(boolean isChecked) {
        mAllDay = isChecked;
        if (isChecked) {
            OControls.setText(mView, R.id.reminderTypeName,
                    String.format(OResource.string(this, R.string.on_the_day_at), "9 AM"));
            findViewById(R.id.event_start_time).setVisibility(View.GONE);
            findViewById(R.id.event_end_time).setVisibility(View.GONE);
        } else {
            OControls.setText(mView, R.id.reminderTypeName, OResource.string(this, R.string.at_the_time_of_event));
            findViewById(R.id.event_start_time).setVisibility(View.VISIBLE);
            findViewById(R.id.event_end_time).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onFieldValueChange(OField field, Object value) {
        if (field.getFieldName().equals("allday"))
            onCheckedChanged(Boolean.parseBoolean(value.toString()));
        if (field.getFieldName().equals("event_date_start")) {
            event_date_end.setValue(value);
        }
        if (field.getFieldName().equals("event_time_start")) {
            event_time_end.setValue(value);
        }
    }

    @Override
    public void onReminderItemSelect(ReminderDialog.ReminderItem value) {
        OControls.setText(mView, R.id.reminderTypeName, value.getTitle());
        mReminder = value;
    }
}