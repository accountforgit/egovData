package com.sugar.zero.egovdata;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class LoadedFileActivity extends AppCompatActivity {

    private ListView fileListView;
    private ArrayList<String[][]> fileList;
    private boolean language=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loaded_file);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");
        fileListView=(ListView)findViewById(R.id.load_file_listview);
        String s= PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getString("langKey","ru");
        if(s.equals("kk"))
            language=true;
        setListAdapter();
    }

    private void setListAdapter(){
        fileList=new ArrayList<String[][]>();
        File list_f[]=getFilesDir().listFiles();
        if(list_f!=null && list_f.length>0 && ((LinearLayout)findViewById(R.id.load_file_layout)).getChildCount()>1)
            ((LinearLayout)findViewById(R.id.load_file_layout)).removeViewAt(0);

        CFile cFile[]=new CFile[list_f.length];
        for(int i=0;i<cFile.length;i++){
            cFile[i]=new CFile(list_f[i],list_f[i].lastModified());
        }
        Arrays.sort(cFile);
        String list[]=new String[cFile.length];
        for(int i=0;i<list.length;i++){
            list[i]=cFile[i].file.getName();
        }
        for(int i=0;i<list.length;i++)
            Log.d("LogA",list[i]);
        for(int i=0;i<list.length-1;i++){
            boolean b=list[i].endsWith(".txt") && list[i+1].endsWith(".json") && list[i].substring(0,list[i].length()-4).equals(list[i+1].substring(0,list[i+1].length()-5));
            if(b){
                String fileList1[][]=new String[1][2];
                fileList1[0][0]=list[i];
                fileList1[0][1]=list[i+1];
                fileList.add(fileList1);
            }
            b=list[i].endsWith(".json") && list[i+1].endsWith(".txt") && list[i].substring(0,list[i].length()-5).equals(list[i+1].substring(0,list[i+1].length()-4));
            if(b){
                String fileList1[][]=new String[1][2];
                fileList1[0][0]=list[i+1];
                fileList1[0][1]=list[i];
                fileList.add(fileList1);
            }
        }

        fileListView.setAdapter(new CustAdapter(getApplicationContext(),R.layout.load_file_cardview));
        fileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent=new Intent(LoadedFileActivity.this, DataLookActivity.class);
                intent.putExtra("fileName",fileList.get(position)[0][0]);
                intent.putExtra("fileNameMeta",fileList.get(position)[0][1]);
                intent.putExtra("from",false);
                startActivity(intent);
            }
        });
        fileListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,final int position, long id) {
                AlertDialog.Builder adlog=new AlertDialog.Builder(LoadedFileActivity.this);
                adlog.setMessage(R.string.are_you_sure_delete);
                adlog.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(new File(getFilesDir(),fileList.get(position)[0][0]).delete())
                            if(new File(getFilesDir(),fileList.get(position)[0][1]).delete())
                                Toast.makeText(LoadedFileActivity.this,R.string.deleted, Toast.LENGTH_SHORT).show();
                                setListAdapter();
                    }
                });
                adlog.setNegativeButton(R.string.cancel, null);
                adlog.create().show();
                return true;
            }
        });
    }

    class CustAdapter extends ArrayAdapter<String>{

        public CustAdapter(Context context, int resource) {
            super(context, resource);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View cardView=getLayoutInflater().inflate(R.layout.load_file_cardview,parent,false);
            String name="";
            String about="";
            try{
                Scanner f=new Scanner(new File(getFilesDir(),fileList.get(position)[0][1]));
                String jsonString="";
                while(f.hasNextLine())
                    jsonString+=f.nextLine();
                JSONObject meta=new JSONObject(jsonString);
                Log.d("LogD",meta.length()+"");
                name=meta.optString("nameRu");
                about=meta.getString("descriptionRu");
                if(language){
                    name=meta.optString("nameKk");
                    about=meta.getString("descriptionKk");
                }
            }
            catch(Exception e){
                Log.d("LogD",e.getMessage()+"");
            }
            ((TextView)cardView.findViewById(R.id.load_file_name)).setText(name);
            ((TextView)cardView.findViewById(R.id.load_file_defination)).setText(about);
            return cardView;
        }

        @Override
        public int getCount() {
            return fileList.size();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.loaded_file,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()==R.id.sort){
            language=!language;
            setListAdapter();
            return true;
        }
        finish();

        return true;
    }

    class CFile implements Comparable{
        public File file;
        public long aLong;
        public CFile(File file,long aLong){
            this.file=file;
            this.aLong=aLong;
        }

        @Override
        public int compareTo(Object another) {
            CFile cFile=(CFile)another;
            if(this.aLong>=cFile.aLong){
                return -1;
            }
            return 1;
        }
    }

}
