package com.kw2.samsung.pt_job;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.beardedhen.androidbootstrap.BootstrapButton;
import com.beardedhen.androidbootstrap.TypefaceProvider;
import com.fsn.cauly.CaulyAdInfo;
import com.fsn.cauly.CaulyAdInfoBuilder;
import com.fsn.cauly.CaulyCloseAd;
import com.fsn.cauly.CaulyCloseAdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.prolificinteractive.materialcalendarview.CalendarDay;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity implements ListAdapter.ListBtnClickListener {
    private CaulyCloseAd closeAd;
    DBHelper helper;
    SQLiteDatabase db;
    ArrayList<ListViewItem> items;
    ListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TypefaceProvider.registerDefaultIconSets();
        setContentView(R.layout.activity_main);

        items = new ArrayList<ListViewItem>();

        // DB 생성
        helper = new DBHelper(this);
        try {
            db = helper.getWritableDatabase();
        } catch (SQLiteException ex) {
            db = helper.getReadableDatabase();
        }

        // Listview 아이템용 DB값 추출

        loadDB(db, items);

        adapter = new ListAdapter(this, R.layout.list_item, items, this);

        ListView listView = (ListView) findViewById(R.id.main_listView);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            // 알바 선택 -> 달력 페이지
            @Override
            public void onItemClick(AdapterView parent, View v, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), PtjDate.class);
                intent.putExtra("ptjId", items.get(position).getPtjId());
                intent.putExtra("ptjName", items.get(position).getPtjName());
                intent.putExtra("ptjMoney", items.get(position).getPtjMoney());
                intent.putExtra("ptjTime", items.get(position).getPtjTime());
                startActivity(intent);
            }
        });

        BootstrapButton plsBtn = (BootstrapButton) findViewById(R.id.main_plsBtn);
        plsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 알바 추가 대화상자
                final Dialog cstDialog = new Dialog(MainActivity.this);
                cstDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                cstDialog.setContentView(R.layout.dialog_ptjsetting);
                WindowManager.LayoutParams params = cstDialog.getWindow().getAttributes();
                params.width = WindowManager.LayoutParams.MATCH_PARENT;
                params.height = WindowManager.LayoutParams.WRAP_CONTENT;
                cstDialog.getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);

                final EditText a_Name = (EditText) cstDialog.findViewById(R.id.setting_ptjName);
                final EditText a_Money = (EditText) cstDialog.findViewById(R.id.setting_ptjMoney);
                final EditText a_TIme = (EditText) cstDialog.findViewById(R.id.setting_ptjTime);

                BootstrapButton cancelBtn = (BootstrapButton) cstDialog.findViewById(R.id.setting_cancelBtn);
                BootstrapButton saveBtn = (BootstrapButton) cstDialog.findViewById(R.id.setting_saveBtn);
                saveBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // 데이터 베이스 insert 문
                        if (a_Name.getText().toString().equals("") || a_Money.getText().toString().equals("") || a_TIme.getText().toString().equals("")) {
                            Toast.makeText(MainActivity.this, "입력사항을 다시 확인해주세요.", Toast.LENGTH_SHORT).show();
                        } else {
                            String name = a_Name.getText().toString();
                            Integer money = Integer.parseInt(a_Money.getText().toString());
                            Integer time = Integer.parseInt(a_TIme.getText().toString());
                            if (name.length() >= 13) {
                                Toast.makeText(MainActivity.this, "알바명을 12자 이내로 입력하세요.", Toast.LENGTH_SHORT).show();
                            } else if (money == 0) {
                                Toast.makeText(MainActivity.this, "시급을 정확히 입력하세요.", Toast.LENGTH_SHORT).show();
                            } else if (time >= 25) {
                                Toast.makeText(MainActivity.this, "하루 평균 근무시간을 입력하세요. (X시간)", Toast.LENGTH_SHORT).show();
                            } else {
                                db.execSQL("INSERT INTO ptJob VALUES (null, '" + name + "', '" + money + "', '" + time + "');");
                                Toast.makeText(getApplication(), "알바 추가 완료", Toast.LENGTH_SHORT).show();
                                cstDialog.dismiss();

                                ListViewItem item = new ListViewItem();
                                item.setPtjId(helper.getInsertId());
                                item.setPtjName(name);
                                item.setPtjMoney(money);
                                item.setPtjTime(time);

                                items.add(item);

                                adapter.notifyDataSetChanged();
                            }
                        }
                    }
                });

                cancelBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        cstDialog.dismiss();
                    }
                });

                cstDialog.show();
            }
        });

        // 종료 광고
        initClose();

        // 하단 배너 광고
        AdView mAdView = (AdView) findViewById(R.id.main_adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        MobileAds.initialize(getApplicationContext(), "ca-app-pub-2916712541842638~6121423593");
    }

    public boolean loadDB(SQLiteDatabase db, ArrayList<ListViewItem> list) {

        ListViewItem item;

        if (list == null) {
            list = new ArrayList<ListViewItem>();
        }

        Cursor cursor = db.rawQuery("SELECT * FROM ptJob", null);
        while (cursor.moveToNext()) {
            item = new ListViewItem();
            item.setPtjId(cursor.getInt(0));
            item.setPtjName(cursor.getString(1));
            item.setPtjMoney(cursor.getInt(2));
            item.setPtjTime(cursor.getInt(3));
            list.add(item);
        }


        return true;
    }

    // 종료시 광고 초기화
    private void initClose(){
        CaulyAdInfo adInfo = new CaulyAdInfoBuilder("GbBLXUpR").build();       // CaulyAdInfo 생성, "CAULY"에 발급 ID 입력
        closeAd =new CaulyCloseAd();                                        // CaulyCloseAd 생성
        closeAd.setAdInfo(adInfo);                                         // CaulyAdView에 AdInfo 적용
        closeAd.setButtonText("아니요","네");                             // 버튼 텍스트 사용자 지정
        closeAd.setDescriptionText("종료 할까요?");                       // 질문 텍스트 사용자 지정
        // 종료 광고 리스너 작성
        closeAd.setCloseAdListener(new CaulyCloseAdListener() {
            // 종료 광고 수신 시
            @Override
            public void onReceiveCloseAd(CaulyCloseAd caulyCloseAd, boolean b) {}

            // 종료 광고가 보여질 시
            @Override
            public void onShowedCloseAd(CaulyCloseAd caulyCloseAd, boolean b) {}

            // 종료 광고 수신 실패 시
            @Override
            public void onFailedToReceiveCloseAd(CaulyCloseAd caulyCloseAd, int i, String s) {}

            // 종료 광고 왼쪽 버튼 클릭 시
            @Override
            public void onLeftClicked(CaulyCloseAd caulyCloseAd) {}

            // 종료 광고 오른쪽 버튼 클릭 시
            @Override
            public void onRightClicked(CaulyCloseAd caulyCloseAd) { finish(); }

            // 광고 클릭으로 앱을 벗어 날 시
            @Override
            public void onLeaveCloseAd(CaulyCloseAd caulyCloseAd) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(closeAd!=null) closeAd.resume(this);  // 종료 광고 구현 시 반드시!! 호출
    }

    // Back Key가 눌러졌을 때, CloseAd 호출
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {  // Back 키이면
            // 앱을 처음 설치하여 실행할 때, 필요한 리소스를 다운받았는지 여부.
            if (closeAd.isModuleLoaded()) {
                // 종료 광고 띄움
                closeAd.show(this);
            } else {
                // 광고에 필요한 리소스를 한번만  다운받는데 실패했을 때 앱의 종료팝업 구현
                showDefaultClosePopup();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    // 기본 종료 팝업
    private void showDefaultClosePopup(){
        new AlertDialog.Builder(this).setTitle("").setMessage("종료 할까요?")
                .setPositiveButton("네", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setNegativeButton("아니요",null)
                .show();
    }

    @Override
    public void onListBtnClick(int id, int option) {
        final int ptjId = id;
        if (option == 0) {
            // 수정 작업
            final Dialog cstDialog = new Dialog(this);
            cstDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            cstDialog.setContentView(R.layout.dialog_ptjsetting);
            WindowManager.LayoutParams params = cstDialog.getWindow().getAttributes();
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            cstDialog.getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);

            final EditText a_Name = (EditText) cstDialog.findViewById(R.id.setting_ptjName);
            final EditText a_Money = (EditText) cstDialog.findViewById(R.id.setting_ptjMoney);
            final EditText a_TIme = (EditText) cstDialog.findViewById(R.id.setting_ptjTime);

            BootstrapButton cancelBtn = (BootstrapButton) cstDialog.findViewById(R.id.setting_cancelBtn);
            BootstrapButton saveBtn = (BootstrapButton) cstDialog.findViewById(R.id.setting_saveBtn);

            final ListViewItem listViewItem = helper.select(id);

            saveBtn.setText("알바 수정");
            a_Name.setText(listViewItem.getPtjName());
            a_Name.setSelection(a_Name.getText().length());
            a_Money.setText(String.valueOf(listViewItem.getPtjMoney()));
            a_TIme.setText(String.valueOf(listViewItem.getPtjTime()));

            // 수정 창 저장
            saveBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String name = a_Name.getText().toString();
                    Integer money = Integer.parseInt(a_Money.getText().toString());
                    Integer time = Integer.parseInt(a_TIme.getText().toString());
                    if (name.length() >= 13) {
                        Toast.makeText(MainActivity.this, "알바명을 12자 이내로 입력하세요.", Toast.LENGTH_SHORT).show();
                    } else if (money == 0) {
                        Toast.makeText(MainActivity.this, "시급을 정확히 입력하세요.", Toast.LENGTH_SHORT).show();
                    } else if (time >= 25) {
                        Toast.makeText(MainActivity.this, "하루 평균 근무시간을 입력하세요. (X시간)", Toast.LENGTH_SHORT).show();
                    } else {
                        helper.update(listViewItem.getPtjId(), a_Name.getText().toString(), Integer.parseInt(a_Money.getText().toString()), Integer.parseInt(a_TIme.getText().toString()));
                        Toast.makeText(getApplicationContext(), "알바 수정 완료", Toast.LENGTH_SHORT).show();
                        cstDialog.dismiss();
                        // items속 해당 값 변경하기
                        for (Iterator<ListViewItem> it = items.iterator(); it.hasNext(); ) {
                            ListViewItem item = it.next();
                            if (ptjId == item.getPtjId()) {
                                item.setPtjName(a_Name.getText().toString());
                                item.setPtjMoney(Integer.parseInt(a_Money.getText().toString()));
                                item.setPtjTime(Integer.parseInt(a_TIme.getText().toString()));
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                }
            });

            // 수정 창 취소
            cancelBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    cstDialog.dismiss();
                }
            });

            cstDialog.show();

        } else if (option == 1) {
            // 삭제 작업
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            AlertDialog alert;
            builder.setTitle("삭제 확인")
                    .setMessage("해당 알바기록을 삭제 하시겠습니까?")
                    .setCancelable(false)
                    .setPositiveButton("확인",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    // 데이터베이스 삭제
                                    helper.delete(ptjId);
                                    Toast.makeText(getApplication(), "삭제 완료", Toast.LENGTH_SHORT).show();

                                    // 해당 값 items에서 삭제
                                    for (Iterator<ListViewItem> it = items.iterator(); it.hasNext(); ) {
                                        ListViewItem item = it.next();
                                        if (ptjId == item.getPtjId()) {
                                            it.remove();
                                        }
                                    }
                                    adapter.notifyDataSetChanged();
                                }
                            })
                    .setNegativeButton("취소",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dialog.cancel();
                                }
                            });
            alert = builder.create();
            alert.show();
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        ListViewItem item = new ListViewItem();
        int lastIndex = items.size();
        items.add(lastIndex, item);
        items.remove(lastIndex);
        adapter.notifyDataSetChanged();
    }
}

class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ptjDB.db";
    private static final int DATABASE_VERSION = 3;
    SQLiteDatabase myDb = this.getWritableDatabase();

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE ptJob ( ptjId INTEGER PRIMARY KEY" + " AUTOINCREMENT, ptjName TEXT, ptjMoney INTEGER, ptjTime INTEGER );");
        db.execSQL("CREATE TABLE timeMoney ( ptjId INTEGER , dYear INTEGER, dMonth INTEGER, dDay INTEGER, dTime INTEGER, dDate INTEGER, dHalf INTEGER default 0 );");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int i1) {
        db.execSQL("ALTER TABLE timeMoney ADD dHalf INTEGER default 0");
    }


    // 해당 알바 이번달 월급
    public String getMonthMoney(int ptjId, int dYear, int dMonth) {
        String str = "";
        Cursor cursor = myDb.rawQuery("select sum(ptJob.ptjMoney * timeMoney.dTime ), sum(timeMoney.dHalf * ptJob.ptjMoney/2) " +
                "from ptJob INNER JOIN timeMoney ON ptJob.ptjId = timeMoney.ptjId " +
                "where ptJob.ptjId=" + ptjId + " and timeMoney.dYear =" + dYear + " and timeMoney.dMonth =" + dMonth, null);
        while (cursor.moveToNext()) {
            int mon = cursor.getInt(0) + cursor.getInt(1);
            String money = String.format("%,d", mon);
            str = String.valueOf(dMonth) + "월 월급 : " + money + "원";

        }

        return str;
    }

    // 해당 알바 총 급여
    public String getAllMoney(int ptjId) {
        String str = "";
        Cursor cursor = myDb.rawQuery("select sum(ptJob.ptjMoney * timeMoney.dTime ), sum(timeMoney.dHalf * ptJob.ptjMoney/2) " +
                "from ptjob INNER JOIN timeMoney ON ptJob.ptjId = timeMoney.ptjId " +
                "where ptJob.ptjId=" + ptjId, null);
        while (cursor.moveToNext()) {
            int mon = cursor.getInt(0) + cursor.getInt(1);
            String money = String.format("%,d", mon);
            str = "총 급여: " + money + "원";
        }
        return str;
    }

    // 해당 알바 총 근무시간
    public String getAllTime(int ptjId) {
        String str = "";
        Cursor cursor = myDb.rawQuery("select sum(timeMoney.dTime ), sum(timeMoney.dHalf) from timeMoney where ptjId=" + ptjId, null);
        while (cursor.moveToNext()) {
            int workTime = cursor.getInt(0) + (cursor.getInt(1) / 2);
            if(cursor.getInt(1)%2 == 0) {
                str = "총 근무시간: " + workTime + "시간 00분";
            }else{
                str = "총 근무시간: " + workTime + "시간 30분";
            }
        }
        return str;
    }

    // 해당 알바 기록 선택
    public ListViewItem select(int ptjId) {
        ListViewItem item = new ListViewItem();
        Cursor cursor = myDb.rawQuery("select * from ptJob where ptjId=" + ptjId + "", null);
        while (cursor.moveToNext()) {
            item.setPtjId(ptjId);
            item.setPtjName(cursor.getString(1));
            item.setPtjMoney(cursor.getInt(2));
            item.setPtjTime(cursor.getInt(3));
        }
        return item;
    }

    // 알바 수정
    public void update(int ptjId, String name, int money, int time) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("ptjId", ptjId);
        contentValues.put("ptjName", name);
        contentValues.put("ptjMoney", money);
        contentValues.put("ptjTime", time);
        myDb.update("ptJob", contentValues, "ptjId = ? ", new String[]{Integer.toString(ptjId)});
    }

    // 알바 삭제
    public void delete(int id) {
        myDb.delete("ptJob", "ptjId = ? ", new String[]{Integer.toString(id)});
    }

    // 마지막 입력 아이디값 검색
    public int getInsertId() {
        Cursor cursor = myDb.rawQuery("select * from ptJob", null);
        cursor.moveToLast();
        return cursor.getInt(0);
    }

    // 해당 날짜 알바 확인
    public boolean dayChecked(int ptjId, int dYear, int dMonth, int dDay) {
        boolean checked = false;
        Cursor cursor = myDb.rawQuery("select * from timeMoney where ptjId=" + ptjId + " and dYear=" + dYear + " and dMonth=" + dMonth + " and dDay=" + dDay + " ", null);

        if (cursor.getCount() != 0) {
            checked = true;
        }

        return checked;
    }

    // 해당 날짜 기록된 알바 시간 확인
    public int getdTime(int ptjId, int dYear, int dMonth, int dDay) {
        int dTIme = 0;
        Cursor cursor = myDb.rawQuery("select dTime from timeMoney where ptjId=" + ptjId + " and dYear=" + dYear + " and dMonth=" + dMonth + " and dDay=" + dDay, null);
        while (cursor.moveToNext()) {
            dTIme = cursor.getInt(0);
        }
        return dTIme;
    }
    // 해당 날짜 기록된 알바 30분 단위 확인
    public int getdHalf(int ptjId, int dYear, int dMonth, int dDay) {
        int dHalf = 0;
        Cursor cursor = myDb.rawQuery("select dHalf from timeMoney where ptjId=" + ptjId + " and dYear=" + dYear + " and dMonth=" + dMonth + " and dDay=" + dDay, null);
        while (cursor.moveToNext()) {
            dHalf = cursor.getInt(0);
        }
        return dHalf;
    }

    // 해당 날짜 알바 기록 입력
    public void insertDate(int ptjId, int dYear, int dMonth, int dDay, int dTime, int dHalf) {
        String dDate = "";
        String dMonthText = "";
        String dDayText = "";

        if (String.valueOf(dMonth).length() == 1) {
            dMonthText = "0" + String.valueOf(dMonth);
        } else {
            dMonthText = String.valueOf(dMonth);
        }

        if (String.valueOf(dDay).length() == 1) {
            dDayText = "0" + String.valueOf(dDay);
        } else {
            dDayText = String.valueOf(dDay);
        }

        dDate = String.valueOf(dYear) + dMonthText + dDayText;

        myDb.execSQL("INSERT INTO timeMoney VALUES ('" + ptjId + "', '" + dYear + "', '" + dMonth + "', '" + dDay + "', '" + dTime + "', '" + dDate + "', '" + dHalf + "');");
    }

    // 해당 날짜 알바 기록 제거
    public void deleteDate(int ptjId, int dYear, int dMonth, int dDay, int dTime, int dHalf) {
        String str[] = {Integer.toString(ptjId), Integer.toString(dYear), Integer.toString(dMonth), Integer.toString(dDay), Integer.toString(dTime), Integer.toString(dHalf)};
        myDb.delete("timeMoney", "ptjId = ? and dYear = ? and dMonth = ? and dDay = ? and dTIme = ? and dHalf = ? ", str);
    }

    // 해당 알바 기록 모두 검색
    public ArrayList<CalendarDay> getDates(int ptjId) {
        ArrayList<CalendarDay> dates = new ArrayList<>();
        Cursor cursor = myDb.rawQuery("select dYear, dMonth, dDay from timeMoney where ptjId=" + ptjId + "", null);
        Calendar calendar = Calendar.getInstance();

        while (cursor.moveToNext()) {
            calendar.add(Calendar.YEAR, cursor.getInt(0) - calendar.get(Calendar.YEAR) );
            calendar.add(Calendar.MONTH, (cursor.getInt(1) - 1) - calendar.get(Calendar.MONTH));
            calendar.add(Calendar.DAY_OF_MONTH, cursor.getInt(2) - calendar.get(Calendar.DAY_OF_MONTH));

            dates.add(CalendarDay.from(calendar));
            calendar = Calendar.getInstance();
        }
        return dates;
    }

    // 기간 검색
    public String getTermValue(int ptjId, String sDate, String eDate) {
        String str = "";

        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
        Date startDate = null;
        Date endDate = null;
        try {
            startDate = format.parse(sDate);
            endDate = format.parse(eDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        ArrayList<Date> dates = new ArrayList<>();
        Cursor cursor = myDb.rawQuery("select dDate from timeMoney where ptjId=" + ptjId + "", null);
        Date date = null;
        while (cursor.moveToNext()) {
            try {
                date = format.parse(String.valueOf(cursor.getInt(0)));
                dates.add(date);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        ArrayList<String> selectDates = new ArrayList<>();

        for (int i = 0; i < dates.size(); i++) {
            if (startDate.compareTo(dates.get(i)) <= 0 && endDate.compareTo(dates.get(i)) >= 0) {
                selectDates.add(format.format(dates.get(i)));
            }
        }
        if (selectDates.size() == 0) {
            str = "0";
        } else {
            String inValue = "";
            String values = "";
            Iterator<String> iterator = selectDates.iterator();
            while (iterator.hasNext()) {
                values = values + iterator.next() + ",";
            }

            inValue = "(" + values.substring(0, values.length() - 1) + ")";
            str = inValue;
        }
        return str;
    }

    public String getTermTime(int ptjId, String term){
        String str = "";
        if(term.length() == 1){
            str = "총 근무시간: 0시간";
        }else{
            Cursor cursor = myDb.rawQuery("select sum(dTime), sum(dHalf) from timeMoney " +
                    "where ptjId=" + ptjId + " and timeMoney.dDate in " + term, null);
            while (cursor.moveToNext()) {
                int timeStr = cursor.getInt(0) + (cursor.getInt(1) / 2);
                if(cursor.getInt(1) % 2 == 0) {
                    str = "총 근무시간: " + timeStr + "시간 00분";
                }else{
                    str = "총 근무시간: " + timeStr + "시간 30분";
                }
            }
        }
        return str;
    }

    public String getTermMoney(int ptjId, String term){
        String str = "";
        if(term.length() == 1){
            str = "총 급여: 0원";
        }else{
            Cursor cursor = myDb.rawQuery("select sum(ptJob.ptjMoney * timeMoney.dTime ), sum(timeMoney.dHalf * ptJob.ptjMoney/2) " +
                    "from ptJob INNER JOIN timeMoney ON ptJob.ptjId = timeMoney.ptjId " +
                    "where ptJob.ptjId=" + ptjId + " and timeMoney.dDate in " + term, null);
            while (cursor.moveToNext()) {
                int mon = cursor.getInt(0) + cursor.getInt(1);
                String money = String.format("%,d", mon);
                str = "총 급여: " + money + "원";
            }
        }
        return str;
    }


}