package com.kw2.samsung.pt_job;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.Toast;

import com.beardedhen.androidbootstrap.BootstrapButton;
import com.beardedhen.androidbootstrap.api.defaults.DefaultBootstrapBrand;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;
import com.prolificinteractive.materialcalendarview.OnMonthChangedListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by SAMSUNG on 2017-12-11.
 */

public class PtjDate extends Activity {

    MaterialCalendarView dateView;
    TextView title, ptjTxt;
    BootstrapButton searchBtn;
    int ptjId, ptjMoney, ptjTime;
    String ptjName, startString, endString;
    EventDecorator decorator;
    DBHelper helper;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ptjdate);

        helper = new DBHelper(getApplicationContext());

        Intent intent = getIntent();
        ptjId = intent.getExtras().getInt("ptjId");
        ptjName = intent.getExtras().getString("ptjName");
        ptjMoney = intent.getExtras().getInt("ptjMoney");
        ptjTime = intent.getExtras().getInt("ptjTime");

        title = (TextView) findViewById(R.id.date_title);
        dateView = (MaterialCalendarView) findViewById(R.id.pra_date);
        ptjTxt = (TextView) findViewById(R.id.date_ptjText);
        searchBtn = (BootstrapButton) findViewById(R.id.date_ptjBtn);

        if (ptjName.length() >= 11) {
            title.setText(ptjName + "\r\n알바 기록");
        } else {
            title.setText(ptjName + " 알바 기록");
        }
        // 달력 밑에 알바 이름, 총근무시간, 총급여, 이번달 급여 표시
        setPtjText();

        // 토,일,오늘 색
        dateView.addDecorators(
                new SundayDecorator(),
                new SaturdayDecorator(),
                new OneDayDecorator());


        //빨간점 스레드 실행
        new ApiSimulator().execute();


        // 달 변경시 계산 값 변경
        dateView.setOnMonthChangedListener(new OnMonthChangedListener() {
            @Override
            public void onMonthChanged(MaterialCalendarView widget, CalendarDay date) {
                setPtjText();
            }
        });


        // 특정 날 클릭
        dateView.setOnDateChangedListener(new OnDateSelectedListener() {
            @Override
            public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date, boolean selected) {
                dateView.clearSelection();
                final CalendarDay day = date;
                final Dialog cstDialog = new Dialog(PtjDate.this);
                cstDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                cstDialog.setContentView(R.layout.dialog_dayselect);

                BootstrapButton plusBtn = (BootstrapButton) cstDialog.findViewById(R.id.daySelect_plsBtn);
                BootstrapButton minBtn = (BootstrapButton) cstDialog.findViewById(R.id.daySelect_minBtn);
                BootstrapButton cancelBtn = (BootstrapButton) cstDialog.findViewById(R.id.daySelect_cancelBtn);
                final BootstrapButton checkBtn = (BootstrapButton) cstDialog.findViewById(R.id.daySelect_checkBtn);

                TextView monthText = (TextView) cstDialog.findViewById(R.id.daySelect_month);
                TextView dayText = (TextView) cstDialog.findViewById(R.id.daySelect_day);
                final TextView timeText = (TextView) cstDialog.findViewById(R.id.daySelect_time);
                // 일급 표시 textview
                final TextView dateText = (TextView) cstDialog.findViewById(R.id.daySelect_dateText);

                monthText.setText(date.getMonth() + 1 + "월");
                dayText.setText(date.getDay() + "일");

                int dateMoney = 0;
                // 해당 날짜 알바기록 있으면 if 없으면 else
                if (helper.dayChecked(ptjId, date.getYear(), date.getMonth() + 1, date.getDay())) {
                    int dTime = helper.getdTime(ptjId, date.getYear(), date.getMonth() + 1, date.getDay());
                    int dHalf = helper.getdHalf(ptjId, date.getYear(), date.getMonth() + 1, date.getDay());
                    if(dHalf == 0) {
                        dateMoney = dTime * ptjMoney;
                    }else {
                        dateMoney = (dTime * ptjMoney) + (ptjMoney/2);
                    }
                    String money = String.format("%,d", dateMoney);
                    if(dHalf == 0) {
                        timeText.setText(String.valueOf(dTime) + "시간 00분");
                    }else{
                        timeText.setText(String.valueOf(dTime) + "시간 30분");
                    }
                    checkBtn.setText("알바 취소");
                    checkBtn.setBootstrapBrand(DefaultBootstrapBrand.DANGER);
                    dateText.setText("일급: " + money + "원");
                    plusBtn.setEnabled(false);
                    minBtn.setEnabled(false);
                } else {
                    dateMoney = ptjTime * ptjMoney;
                    String money = String.format("%,d", dateMoney);
                    dateText.setText("일급: " + money + "원");
                    timeText.setText(String.valueOf(ptjTime) + "시간 00분");
                    checkBtn.setText("알바 완료");
                }

                View.OnClickListener listener = new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int timeInt = 0;
                        int dateMoney = 0;
                        String halfStr = "";
                        switch (view.getId()) {
                            case R.id.daySelect_minBtn:
                                timeInt = Integer.parseInt(timeText.getText().toString().substring(0, timeText.getText().toString().length() - 6));
                                halfStr = timeText.getText().toString().substring(timeText.getText().toString().length() - 3);
                                if(halfStr.equals("30분")){
                                    halfStr = "00분";
                                    dateMoney = ptjMoney * timeInt;
                                }else{
                                    halfStr = "30분";
                                    if (timeInt == 1) {
                                        timeInt = 24;
                                        halfStr = "00분";
                                    } else {
                                        timeInt = timeInt - 1;
                                    }
                                    dateMoney = (ptjMoney * timeInt) + ptjMoney/2;
                                }
                                timeText.setText(String.valueOf(timeInt) + "시간 " + halfStr);
                                dateText.setText("일급: " + dateMoney + "원");
                                break;
                            case R.id.daySelect_plsBtn:
                                timeInt = Integer.parseInt(timeText.getText().toString().substring(0, timeText.getText().toString().length() - 6));
                                halfStr = timeText.getText().toString().substring(timeText.getText().toString().length() - 3);
                                if(halfStr.equals("30분")) {
                                    if (timeInt == 24) {
                                        timeInt = 1;
                                    } else {
                                        timeInt = timeInt + 1;
                                    }
                                    halfStr = "00분";
                                    dateMoney = ptjMoney * timeInt;
                                }else{
                                    halfStr = "30분";
                                    if (timeInt == 24) {
                                        timeInt = 1;
                                        halfStr = "00분";
                                    }
                                    dateMoney = (ptjMoney * timeInt) + ptjMoney/2;
                                }
                                timeText.setText(String.valueOf(timeInt) + "시간 " + halfStr);
                                dateText.setText("일급: " + dateMoney + "원");
                                break;
                        }
                    }
                };

                plusBtn.setOnClickListener(listener);
                minBtn.setOnClickListener(listener);


                checkBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // 달력에 체크 확인/해제
                        String timeTxt = timeText.getText().toString().substring(0,timeText.getText().toString().length()-6 );
                        String halfTxt = timeText.getText().toString().substring(timeText.getText().toString().length()-3);
                        int halfInt = 0;
                        if(halfTxt.equals("30분")){
                            halfInt = 1;
                        }
                        if (checkBtn.getText().equals("알바 완료")) {
                            helper.insertDate(ptjId, day.getYear(), day.getMonth() + 1, day.getDay(), Integer.parseInt(timeTxt), halfInt);
                            dateView.removeDecorator(decorator);
                            new ApiSimulator().execute();
                            cstDialog.dismiss();
                            setPtjText();
                        } else {
                            helper.deleteDate(ptjId, day.getYear(), day.getMonth() + 1, day.getDay(), Integer.parseInt(timeTxt), halfInt);
                            dateView.removeDecorator(decorator);
                            new ApiSimulator().execute();
                            cstDialog.dismiss();
                            setPtjText();
                        }
                    }
                });

                cancelBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        cstDialog.dismiss();
                        dateView.clearSelection();
                    }
                });

                cstDialog.show();
            }
        });

        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Dialog cstDialog = new Dialog(PtjDate.this);
                cstDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                cstDialog.setContentView(R.layout.dialog_searchterm);
                WindowManager.LayoutParams params = cstDialog.getWindow().getAttributes();
                params.width = WindowManager.LayoutParams.MATCH_PARENT;
                params.height = WindowManager.LayoutParams.WRAP_CONTENT;
                cstDialog.getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);

                BootstrapButton startBtn = (BootstrapButton) cstDialog.findViewById(R.id.term_startBtn);
                BootstrapButton endBtn = (BootstrapButton) cstDialog.findViewById(R.id.term_endBtn);

                final TextView startText = (TextView) cstDialog.findViewById(R.id.term_startText);
                final TextView endText = (TextView) cstDialog.findViewById(R.id.term_endText);

                BootstrapButton sBtn = (BootstrapButton) cstDialog.findViewById(R.id.search_searchBtn);
                BootstrapButton eBtn = (BootstrapButton) cstDialog.findViewById(R.id.search_endBtn);

                final TextView sTimeText = (TextView) cstDialog.findViewById(R.id.search_timeText);
                final TextView sMoneyText = (TextView) cstDialog.findViewById(R.id.search_moneyText);


                View.OnClickListener listener = new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        DatePickerDialog datePickerDialog;
                        DatePickerDialog.OnDateSetListener dateSetListener;
                        final Calendar c = Calendar.getInstance();
                        int mYear = c.get(Calendar.YEAR);
                        int mMonth = c.get(Calendar.MONTH);
                        int mDay = c.get(Calendar.DAY_OF_MONTH);

                        switch (view.getId()) {
                            case R.id.term_startBtn:
                                dateSetListener = new DatePickerDialog.OnDateSetListener() {
                                    @Override
                                    public void onDateSet(DatePicker datePicker, int yearSelected, int monthOfYear, int dayOfMonth) {
                                        monthOfYear = monthOfYear + 1;
                                        startText.setText(yearSelected + "년 " + monthOfYear + "월 " + dayOfMonth + "일");
                                        String monthTxt = null;
                                        String dayTxt = null;
                                        if(String.valueOf(monthOfYear).length() == 1){
                                            monthTxt = "0" + String.valueOf(monthOfYear);
                                        }else{
                                            monthTxt = String.valueOf(monthOfYear);
                                        }
                                        if(String.valueOf(dayOfMonth).length() == 1){
                                            dayTxt = "0" + String.valueOf(dayOfMonth);
                                        }else{
                                            dayTxt = String.valueOf(dayOfMonth);
                                        }

                                        startString = String.valueOf(yearSelected) + monthTxt + dayTxt;
                                    }
                                };
                                datePickerDialog = new DatePickerDialog(PtjDate.this, dateSetListener, mYear, mMonth, mDay);
                                datePickerDialog.show();
                                break;
                            case R.id.term_endBtn:
                                dateSetListener = new DatePickerDialog.OnDateSetListener() {
                                    @Override
                                    public void onDateSet(DatePicker datePicker, int yearSelected, int monthOfYear, int dayOfMonth) {
                                        monthOfYear = monthOfYear + 1;
                                        endText.setText(yearSelected + "년 " + monthOfYear + "월 " + dayOfMonth + "일");
                                        String monthTxt = null;
                                        String dayTxt = null;
                                        if(String.valueOf(monthOfYear).length() == 1){
                                            monthTxt = "0" + String.valueOf(monthOfYear);
                                        }else{
                                            monthTxt = String.valueOf(monthOfYear);
                                        }
                                        if(String.valueOf(dayOfMonth).length() == 1){
                                            dayTxt = "0" + String.valueOf(dayOfMonth);
                                        }else{
                                            dayTxt = String.valueOf(dayOfMonth);
                                        }
                                        endString = String.valueOf(yearSelected) + monthTxt + dayTxt;
                                    }
                                };
                                datePickerDialog = new DatePickerDialog(PtjDate.this, dateSetListener, mYear, mMonth, mDay);
                                datePickerDialog.show();
                                break;
                        }
                    }
                };

                startBtn.setOnClickListener(listener);
                endBtn.setOnClickListener(listener);

                sBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (startString == null || endString == null || checkDate(startString, endString)) {
                            Toast.makeText(PtjDate.this, "시작일과 종료일을 다시 설정해 주세요.", Toast.LENGTH_SHORT).show();
                        } else {
                            String term = helper.getTermValue(ptjId, startString, endString);
                            sTimeText.setText(helper.getTermTime(ptjId, term));
                            sMoneyText.setText(helper.getTermMoney(ptjId,term));
                            Toast.makeText(PtjDate.this, "검색 완료", Toast.LENGTH_SHORT).show();
                            startString = null;
                            endString = null;
                        }
                    }
                });

                eBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        cstDialog.dismiss();
                    }
                });

                cstDialog.show();
            }
        });

        AdView mAdView = (AdView) findViewById(R.id.date_adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        MobileAds.initialize(PtjDate.this, "ca-app-pub-2916712541842638~6121423593");
    }

    public void setPtjText() {
        ptjTxt.setText(ptjName + "\r\n" + helper.getAllTime(ptjId) + "\r\n" + helper.getAllMoney(ptjId) + "\r\n"
                + helper.getMonthMoney(ptjId, dateView.getCurrentDate().getYear(), dateView.getCurrentDate().getMonth() + 1));
    }

    public boolean checkDate(String sDate, String eDate) {
        boolean check = false;
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
        Date startDate = null;
        Date endDate = null;
        try {
            startDate = format.parse(sDate);
            endDate = format.parse(eDate);
        } catch (ParseException e) {
            check = true;
        }
        if(startDate != null && endDate != null) {
            if (startDate.compareTo(endDate) >= 0) {
                check = true;
            }
        }

        return check;
    }

    private class ApiSimulator extends AsyncTask<Void, Void, List<CalendarDay>> {

        @Override
        protected List<CalendarDay> doInBackground(@NonNull Void... voids) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            ArrayList<CalendarDay> dates = helper.getDates(ptjId);
            return dates;
        }

        @Override
        protected void onPostExecute(@NonNull List<CalendarDay> calendarDays) {
            super.onPostExecute(calendarDays);

            if (isFinishing()) {
                return;
            }
            decorator = new EventDecorator(Color.RED, calendarDays);
            dateView.addDecorator(decorator);
        }
    }


}


