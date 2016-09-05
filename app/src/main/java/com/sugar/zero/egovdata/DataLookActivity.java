package com.sugar.zero.egovdata;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;

public class DataLookActivity extends AppCompatActivity {

    private String fileName;
    private String fileNameMeta;
    private boolean language;
    private ArrayList<JSONObject> itemList;
    private HashMap<String,String> metaDataList;
    private ArrayList<JSONObject> dataList;
    private ArrayList<String> realName;
    private ListView dataListView;
    private File rootFile;
    private SearchView searchView;
    private int col=4;
    private int sortPosition=-1;
    private boolean sortDir=true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_look);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle("");
        dataListView=(ListView)findViewById(R.id.data_list_view);
        fileName=getIntent().getStringExtra("fileName");
        fileNameMeta=getIntent().getStringExtra("fileNameMeta");
        boolean from=getIntent().getBooleanExtra("from",false);
        if(from)
            rootFile=getCacheDir();
        else
            rootFile=getFilesDir();
        itemList=new ArrayList<JSONObject>();
        dataList=itemList;
        metaDataList=new HashMap<String,String>();
        realName=new ArrayList<String>();
        String s= PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getString("langKey","ru");
        if(s.equals("kk"))
            language=true;
        setMetaData();
        setItemList();
        setMetaLayout();
        setDataListView();
    }

    private void setMetaData() {
        try {
            Scanner f=new Scanner(new File(rootFile,fileNameMeta));
            String s="";
            while (f.hasNextLine())
                s+=f.nextLine();
            Log.d("LogA",s+"");
            JSONObject j2=new JSONObject(s).getJSONObject("fields");
            JSONArray j1=new JSONObject(s).getJSONObject("fields").names();
            for(int i=0;i<j1.length();i++){
                String scname=j1.get(i).toString();
                String name=j2.getJSONObject(scname).getString("labelRu");
                Log.d("LogA", name + " " + scname);
                if(language){
                    name=j2.getJSONObject(scname).getString("labelKk");
                }
                metaDataList.put(scname, name);
            }

        }
        catch (Exception e){
            Log.d("LogErr",e.getMessage()+"");
        }
    }

    private void setMetaLayout() {
        LinearLayout linearLayout=(LinearLayout)findViewById(R.id.data_meta_layout);
        linearLayout.removeAllViews();
        for(int i=0;i<col && i<realName.size();i++){
            TextView view=(TextView)getLayoutInflater().inflate(R.layout.data_meta_item,linearLayout,false);
            String s=metaDataList.get(realName.get(i)) == null ? realName.get(i) : metaDataList.get(realName.get(i));
            if(s.length()<13)
                s+='\n';
            if(s.length()>26)
                s=s.substring(0,24)+"...";
            view.setText(s);
            view.setTextSize(15);
            view.setTag(i);
            linearLayout.addView(view);
        }
    }

    private void setItemList(){
        try{
            Scanner f=new Scanner(new File(rootFile,fileName));
            String s="";
            while (f.hasNextLine())
                s+=f.nextLine();
            Log.d("LogD",s+" sd ");
            JSONArray array=new JSONArray(s);
            if(array.length()>0){
                JSONArray j1=array.getJSONObject(0).names();
                for(int i=0;i<j1.length();i++){
                    String name=j1.get(i).toString();
                    realName.add(name);
                }
            }
            for(int i=0;i<array.length();i++){
                itemList.add(array.getJSONObject(i));
            }
        }
        catch(Exception e){}
    }

    private void setDataListView() {
        if(itemList.size() ==0){
            Toast.makeText(DataLookActivity.this,R.string.empty, Toast.LENGTH_SHORT).show();
        }
        else{
            setQueryList(itemList);
            Toast.makeText(DataLookActivity.this,getString(R.string.total)+" "+itemList.size(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.data_look, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setQueryHint(getString(R.string.search));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                dataList = null;
                if (newText.equals(""))
                    dataList = itemList;
                else {
                    dataList = new ArrayList<JSONObject>();
                    for (int i = 0; i < itemList.size(); i++) {
                        JSONObject j1 = itemList.get(i);
                        int j = 0;
                        while (j < realName.size()) {
                            String data = "-----";
                            try {
                                data = j1.getString(realName.get(j));
                            } catch (Exception e) {
                            }
                            if (data.toLowerCase().contains(newText.toLowerCase())) {
                                dataList.add(j1);
                                break;
                            }
                            j++;
                        }
                    }
                }
                if (sortPosition != -1)
                    sortList(dataList, sortPosition);
                else
                    setQueryList(dataList);
                return true;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    private void sortList(final ArrayList<JSONObject> dataList,final int p) {
        AlertDialog.Builder adlog=new AlertDialog.Builder(DataLookActivity.this);
        adlog.setMessage(R.string.loading);
        adlog.setCancelable(false);
        final Dialog dialog=adlog.create();
        if(dataList.size()>100)
            dialog.show();
        new AsyncTask<String,String,String>(

        ){
            @Override
            protected String doInBackground(String... params) {
                int n=dataList.size();
                for(int i=1;i<n;i++){
                    for(int j=i;j>0;j--){
                        String s1="-----";
                        String s2="-----";
                        try{
                            s1=dataList.get(j).getString(realName.get(p));
                        }catch(Exception e){}
                        try{
                            s2=dataList.get(j-1).getString(realName.get(p));
                        }catch(Exception e){}
                        if(!sortDir){
                            String s3=s1;
                            s1=s2;
                            s2=s3;
                        }
                        if(s1.toLowerCase().compareTo(s2.toLowerCase())<0){
                            JSONObject j1=dataList.get(j);
                            dataList.set(j, dataList.get(j - 1));
                            dataList.set(j - 1, j1);
                        }
                        else
                            break;
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                setQueryList(dataList);
                dialog.cancel();
            }
        }.execute();
    }

    public void onSortClick(View v){
        int p=Integer.parseInt(v.getTag().toString());
        if(sortPosition!=-1){
            if(p!=sortPosition)
                sortDir=true;
            else
                sortDir=!sortDir;
        }
        sortPosition=p;
        sortList(dataList,p);
    }

    private synchronized void setQueryList(final ArrayList<JSONObject> dataList){
        dataListView.setAdapter(new CustArrayAdapter(getApplicationContext(), R.layout.data_cardview, dataList));
        dataListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AlertDialog.Builder adlog = new AlertDialog.Builder(DataLookActivity.this);
                adlog.setTitle(R.string.fullinfo);
                View detailInfo = getLayoutInflater().inflate(R.layout.data_look_detail_info, null);
                LinearLayout linearLayout = (LinearLayout) detailInfo.findViewById(R.id.data_look_datail_layout);
                try {
                    Iterator<String> fore = dataList.get(position).keys();
                    while (fore.hasNext()) {
                        String key = fore.next();
                        LinearLayout linearLayout1 = (LinearLayout) getLayoutInflater().inflate(R.layout.data_look_detail_subinfo, linearLayout, false);
                        ((TextView) linearLayout1.findViewById(R.id.data_look_detail_name)).setText(metaDataList.get(key) == null ? key : metaDataList.get(key));
                        String data = "-----";
                        data = dataList.get(position).getString(key);
                        ((TextView) linearLayout1.findViewById(R.id.data_look_detail_value)).setText(data);
                        linearLayout.addView(linearLayout1);
                    }
                } catch (Exception e) {
                }
                adlog.setView(detailInfo);
                adlog.setNegativeButton(R.string.cancel, null);
                adlog.create().show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()==R.id.language){
            language=!language;
            setMetaData();
            setMetaLayout();
            return true;
        }
        if(item.getItemId()==android.R.id.home)
            finish();
        if(item.getItemId()==R.id.should){
            AlertDialog.Builder adlog=new AlertDialog.Builder(DataLookActivity.this);
            adlog.setView(getLayoutInflater().inflate(R.layout.should_layout,null));
            adlog.create().show();
        }
        return true;
    }

    class CustArrayAdapter extends ArrayAdapter<String> {

        private ArrayList<JSONObject> dataList;

        public CustArrayAdapter(Context context, int resource,ArrayList<JSONObject> dataList) {
            super(context, resource);
            this.dataList=dataList;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View cardView=getLayoutInflater().inflate(R.layout.data_cardview,parent,false);
            LinearLayout linearLayout=(LinearLayout)cardView.findViewById(R.id.data_cardview_layout);
            JSONObject j1=dataList.get(position);
            int j=0;
            while(j<realName.size() && j<col){
                TextView textView=(TextView)getLayoutInflater().inflate(R.layout.data_meta_textview,linearLayout,false);
                String data="-----";
                try {
                    data=j1.getString(realName.get(j));
                }
                catch (Exception e){}
                if(data.length()<13)
                    data+='\n';
                if(data.length()>52)
                    data=data.substring(0,50)+"...";
                textView.setText(data+"");
                linearLayout.addView(textView);
                j++;
            }
            return cardView;
        }

        @Override
        public int getCount() {
            return dataList.size();
        }
    }

}
