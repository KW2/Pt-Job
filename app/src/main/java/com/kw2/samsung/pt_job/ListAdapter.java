package com.kw2.samsung.pt_job;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.beardedhen.androidbootstrap.BootstrapButton;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by SAMSUNG on 2017-12-10.
 */

public class ListAdapter extends ArrayAdapter<ListViewItem> implements View.OnClickListener {

    public interface ListBtnClickListener {
        void onListBtnClick(int id, int option) ;
    }

    private ListBtnClickListener listBtnClickListener ;
    private final Activity context;
    int resourceId ;
    private DBHelper helper;

    public ListAdapter(Activity context, int resource , ArrayList<ListViewItem> list, ListBtnClickListener clickListener){
        super(context, resource, list);
        this.context = context;
        this.resourceId = resource;
        this.listBtnClickListener = clickListener;
        helper = new DBHelper(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(this.resourceId, parent, false);
        }

        final TextView nameText = (TextView) convertView.findViewById(R.id.item_ptjName);
        final TextView moneyTextAll = (TextView) convertView.findViewById(R.id.item_moneyAll);
        final TextView moneyTextMonth = (TextView) convertView.findViewById(R.id.item_moneyMonth);
        final ListViewItem listViewItem = (ListViewItem) getItem(position);

        Calendar calendar = Calendar.getInstance();

        nameText.setText(listViewItem.getPtjName());
        moneyTextAll.setText(helper.getAllMoney(listViewItem.getPtjId()));
        moneyTextMonth.setText(helper.getMonthMoney(listViewItem.getPtjId(), calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH)+1));


        BootstrapButton updaateBtn = (BootstrapButton) convertView.findViewById(R.id.item_updBtn);
        BootstrapButton deleteBtn = (BootstrapButton) convertView.findViewById(R.id.item_delBtn);

        updaateBtn.setTag(listViewItem.getPtjId());
        deleteBtn.setTag(listViewItem.getPtjId());

        updaateBtn.setOnClickListener(this);
        deleteBtn.setOnClickListener(this);

        return convertView;
    }

    @Override
    public void onClick(View view) {
        if (this.listBtnClickListener != null) {
            switch (view.getId()){
                case R.id.item_updBtn:
                    this.listBtnClickListener.onListBtnClick((int)view.getTag(), 0) ;
                    break;
                case R.id.item_delBtn:
                    this.listBtnClickListener.onListBtnClick((int)view.getTag(), 1) ;
                    break;
            }

        }
    }
}
