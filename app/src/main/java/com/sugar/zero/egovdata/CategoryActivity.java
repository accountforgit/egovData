package com.sugar.zero.egovdata;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;

public class CategoryActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private String titleList[];
    private String linkList[];
    private String language_name[];
    private String sort_value[];
    private static final String USER_AGENT = "Mozilla/5.0";
    private  Dialog languageDialog;
    private int languagePosition=0;
    private int sortPosition=0;
    private int linkPosition=0;
    private boolean byGovAgencies=true;
    private boolean byExternalUsers=true;
    private boolean isSelected=false;
    private boolean isLast=false;
    private RecyclerView mRecyclerView;
    private DataAdapter mDataAdapter;
    private ArrayList<JsonData> jsonData = new ArrayList<>();
    private int page=2;
    private String s;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        s=PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getString("langKey","ru");
        if(s.equals("kk")){
            if(!getResources().getConfiguration().locale.getLanguage().equals("kk")){
                Resources res = getBaseContext().getResources();
                // Change locale settings in the app.
                DisplayMetrics dm = res.getDisplayMetrics();
                android.content.res.Configuration conf = res.getConfiguration();
                conf.locale = new Locale("kk".toLowerCase());
                res.updateConfiguration(conf, dm);
                Intent intent = getIntent();
                finish();
                startActivity(intent);
            }
            languagePosition=1;
            ((ImageView)navigationView.getHeaderView(0).findViewById(R.id.logo)).setImageResource(R.drawable.logodatakk);
        }
        else
            ((ImageView)navigationView.getHeaderView(0).findViewById(R.id.logo)).setImageResource(R.drawable.logodataru);
        byGovAgencies=PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getBoolean("govKey",true);
        byExternalUsers=PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getBoolean("outKey",true);
        navigationView.setNavigationItemSelectedListener(this);
        titleList=getResources().getStringArray(R.array.category_name);
        linkList=getResources().getStringArray(R.array.category_link);
        sort_value=getResources().getStringArray(R.array.sort_value);
        ArrayAdapter
                <CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.sort_name, R.layout.layout_drop_title);
        adapter.setDropDownViewResource(R.layout.layout_drop_list);
        Spinner mNavigationSpinner = new Spinner(getSupportActionBar().getThemedContext());
        mNavigationSpinner.setAdapter(adapter);
        toolbar.addView(mNavigationSpinner);
        mNavigationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isSelected) {
                    sortPosition = position;
                    setDataset();
                }
                sortPosition = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mRecyclerView = (RecyclerView) findViewById(R.id.recycleView);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setDataAdapter(final String text) {
        jsonData=new ArrayList<JsonData>();
        jsonParser(text);
        page=2;
        mDataAdapter = new DataAdapter();
        mRecyclerView.removeAllViews();
        mRecyclerView.setAdapter(mDataAdapter);
        mDataAdapter.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore() {
                if (!isLast && jsonData.size() != 0 && jsonData.get(jsonData.size() - 1) != null) {
                    new AsyncTask<String, String, String>() {

                        String loadText;

                        @Override
                        protected void onPreExecute() {
                            super.onPreExecute();
                            Log.e("haint", "Load More");
                            jsonData.add(null);
                            mDataAdapter.notifyItemInserted(jsonData.size() - 1);
                        }

                        @Override
                        protected String doInBackground(String... params) {
                            loadText = "";
                            try {
                                loadText = httpLoader("page=" + page + "&count=10&byGovAgencies=" + byGovAgencies + "&byExternalUsers=" + byExternalUsers + "&sortBy="+sort_value[sortPosition]);
                                if (!loadText.equals(""))
                                    page++;
                            } catch (Exception e) {
                            }
                            return null;
                        }

                        @Override
                        protected void onPostExecute(String s) {
                            super.onPostExecute(s);
                            jsonData.remove(jsonData.size() - 1);
                            jsonParser(loadText);
                            Log.d("LogD", jsonData.size() + "");
                            mDataAdapter.notifyDataSetChanged();
                            mDataAdapter.setLoaded();
                        }
                    }.execute();
                }
            }
        });
    }

    private synchronized void jsonParser(String text) {
        if(text==null)
            Toast.makeText(CategoryActivity.this, R.string.error_loading, Toast.LENGTH_LONG).show();
        try{
            JSONObject root=new JSONObject(text);
            JSONArray jsonArray = root.optJSONArray("datasets");
            Log.d("LogD",jsonArray.length()+"");
            if(jsonArray.length()<10){
                isLast=true;
            }
            for(int i=0; i < jsonArray.length(); i++){
                JsonData jsonItem=new JsonData();
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                jsonItem.apiUri = jsonObject.optString("apiUri");
                jsonItem.name=jsonObject.optString("nameRu");
                jsonItem.defination=jsonObject.optString("descriptionRu");
                try{
                    jsonItem.createDate=jsonObject.optString("createdDate");
                    jsonItem.modDate=jsonObject.optString("modifiedDate");
                    jsonItem.view=jsonObject.optString("views");
                    jsonItem.dataSetCount=jsonObject.optString("datasetsCount");
                    jsonItem.tel=jsonObject.getJSONObject("responsible").optString("phone");
                    jsonItem.mail=jsonObject.getJSONObject("responsible").optString("email");
                    jsonItem.owner=jsonObject.getJSONObject("owner").optString("fullnameRu");

                }
                catch (Exception e){
                    Log.d("Err",e.getMessage()+"");
                }

                if(languagePosition==1){
                    jsonItem.name=jsonObject.optString("nameKk");
                    jsonItem.defination=jsonObject.optString("descriptionKk");
                }
                jsonData.add(jsonItem);
            }

        }
        catch (Exception e){
            Log.d("Err",e.getMessage()+"");
        }
    }

    public void onCardViewClick(View v){
        int position=Integer.parseInt(v.getTag().toString());
        AlertDialog.Builder adlog=new AlertDialog.Builder(CategoryActivity.this);
        View dialogView=getLayoutInflater().inflate(R.layout.data_detail_info,null);
        ((TextView)dialogView.findViewById(R.id.detail_name)).setText(jsonData.get(position).name);
        ((TextView)dialogView.findViewById(R.id.detail_about)).setText(jsonData.get(position).defination);
        ((TextView)dialogView.findViewById(R.id.detail_owner)).setText(jsonData.get(position).owner.equals("") ? getString(R.string.noinfo) : jsonData.get(position).owner);
        ((TextView)dialogView.findViewById(R.id.detail_category)).setText(titleList[linkPosition]);
        ((TextView)dialogView.findViewById(R.id.detail_add_date)).setText(jsonData.get(position).createDate);
        ((TextView)dialogView.findViewById(R.id.detail_mod_date)).setText(jsonData.get(position).modDate);
        ((TextView)dialogView.findViewById(R.id.detail_tel)).setText(jsonData.get(position).tel.equals("") ? getString(R.string.noinfo) : jsonData.get(position).tel);
        ((TextView)dialogView.findViewById(R.id.detail_mail)).setText(jsonData.get(position).mail.equals("") ? getString(R.string.noinfo): jsonData.get(position).mail);
        dialogView.findViewById(R.id.detail_download).setTag(position);
        dialogView.findViewById(R.id.detail_look).setTag(position);
        adlog.setTitle(R.string.password);
        adlog.setView(dialogView);
        adlog.setNegativeButton(R.string.cancel,null);
        adlog.show();
    }

    private boolean isLook;
    private File rootFile;

    public void downloadOnClick(View v){
        int p=Integer.parseInt(v.getTag().toString());
        isLook=false;
        downloadAsynk(p,getFilesDir());
    }

    public void lookOnClick(View v){
        int p=Integer.parseInt(v.getTag().toString());
        isLook=true;
        downloadAsynk(p, getCacheDir());
    }

    private void downloadAsynk(final int position,final File rootFile){
        new AsyncTask<String,String,String>(){
            boolean isSuccess=true;
            Dialog loadDialog;
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                AlertDialog.Builder adlog=new AlertDialog.Builder(CategoryActivity.this);
                adlog.setMessage(R.string.loading);
                adlog.setCancelable(false);
                loadDialog=adlog.create();
                loadDialog.show();
            }

            @Override
            protected String doInBackground(String... params) {
                try{
                    String ts="";
                    int from=1;
                    while(true){
                        HttpsURLConnection con=(HttpsURLConnection) new URL("https://data.egov.kz/datasets/exportjson?index="+jsonData.get(position).apiUri+"&version=v1&from="+from+"&count=100").openConnection();
                        BufferedInputStream bif=new BufferedInputStream(con.getInputStream());
                        Scanner a=new Scanner(bif);
                        String s1="";
                        while(a.hasNextLine())
                            s1+=a.nextLine();
                        from+=100;
                        if(!s1.equals("[]")){
                            if(ts.equals(""))
                                ts=s1;
                            else
                                ts=ts.substring(0,ts.length()-1)+", "+s1.substring(1);
                        }
                        else
                            break;
                        if(s1.equals(""))
                            break;
                    }
                    if(ts.equals("")) {
                        from=1;
                        while (true) {
                            HttpsURLConnection con = (HttpsURLConnection) new URL("https://data.egov.kz/datasets/exportjson?index=" + jsonData.get(position).apiUri + "&version=data&from=" + from + "&count=100").openConnection();
                            BufferedInputStream bif = new BufferedInputStream(con.getInputStream());
                            Scanner a = new Scanner(bif);
                            String s1 = "";
                            while (a.hasNextLine())
                                s1 += a.nextLine();
                            from += 100;
                            if (!s1.equals("[]")) {
                                if (ts.equals(""))
                                    ts = s1;
                                else
                                    ts = ts.substring(0, ts.length() - 1) + ", " + s1.substring(1);
                            } else
                                break;
                            if(s1.equals(""))
                                break;
                        }
                    }
                    BufferedOutputStream buf=new BufferedOutputStream(new FileOutputStream(new File(rootFile.getAbsoluteFile()+"",jsonData.get(position).apiUri+".txt")));
                    buf.write(ts.getBytes());
                    buf.flush();
                    buf.close();
                    int c;
                    byte data[];
                    HttpsURLConnection conmeta=(HttpsURLConnection) new URL("https://data.egov.kz/meta/"+jsonData.get(position).apiUri+"/v1").openConnection();
                    BufferedOutputStream bufmeta=new BufferedOutputStream(new FileOutputStream(new File(rootFile.getAbsoluteFile()+"",jsonData.get(position).apiUri+".json")));
                    BufferedInputStream bifmeta=new BufferedInputStream(conmeta.getInputStream());
                    data=new byte[100];
                    while((c=bifmeta.read(data,0,100))>0){
                        bufmeta.write(data,0,c);
                        bufmeta.flush();
                    }
                    bufmeta.close();
                }
                catch(Exception e){
                    isSuccess=false;
                }
                return null;
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                try {
                    loadDialog.cancel();
                }
                catch (Exception e){}
                if(!isLook)
                    if(isSuccess)
                        Toast.makeText(CategoryActivity.this,R.string.loading_end, Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(CategoryActivity.this, R.string.cant_load, Toast.LENGTH_SHORT).show();
                else{
                    if(isSuccess){
                        Intent intent=new Intent(CategoryActivity.this,DataLookActivity.class);
                        intent.putExtra("fileName",jsonData.get(position).apiUri+".txt");
                        intent.putExtra("fileNameMeta",jsonData.get(position).apiUri+".json");
                        intent.putExtra("from",true);
                        startActivity(intent);
                    }
                }
            }

        }.execute();
    }

    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        public ProgressBar progressBar;

        public LoadingViewHolder(View itemView) {
            super(itemView);
            progressBar = (ProgressBar) itemView.findViewById(R.id.progressBar1);
        }
    }

    static class DataViewHolder extends RecyclerView.ViewHolder {
        public TextView tvName;
        public TextView tvDefination;
        public TextView tvDate;
        public TextView tvView;

        public DataViewHolder(View itemView) {
            super(itemView);

            tvName = (TextView) itemView.findViewById(R.id.tvName);
            tvDefination = (TextView) itemView.findViewById(R.id.tvDefination);
            tvDate=(TextView)itemView.findViewById(R.id.date);
            tvView=(TextView)itemView.findViewById(R.id.view_count);
        }
    }

    class DataAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private final int VIEW_TYPE_ITEM = 0;
        private final int VIEW_TYPE_LOADING = 1;

        private OnLoadMoreListener mOnLoadMoreListener;

        private boolean isLoading;
        private int visibleThreshold = 5;
        private int lastVisibleItem, totalItemCount;

        public DataAdapter() {
            final LinearLayoutManager linearLayoutManager = (LinearLayoutManager) mRecyclerView.getLayoutManager();
            mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);

                    totalItemCount = linearLayoutManager.getItemCount();
                    lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition();

                    if (!isLoading && totalItemCount <= (lastVisibleItem + visibleThreshold)) {
                        if (mOnLoadMoreListener != null) {
                            mOnLoadMoreListener.onLoadMore();
                        }
                        isLoading = true;
                    }
                }
            });
        }

        public void setOnLoadMoreListener(OnLoadMoreListener mOnLoadMoreListener) {
            this.mOnLoadMoreListener = mOnLoadMoreListener;
        }

        @Override
        public int getItemViewType(int position) {
            return jsonData.get(position) == null ? VIEW_TYPE_LOADING : VIEW_TYPE_ITEM;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == VIEW_TYPE_ITEM) {
                View view = LayoutInflater.from(CategoryActivity.this).inflate(R.layout.layout_data_item, parent, false);
                return new DataViewHolder(view);
            } else if (viewType == VIEW_TYPE_LOADING) {
                View view = LayoutInflater.from(CategoryActivity.this).inflate(R.layout.layout_loading_item, parent, false);
                return new LoadingViewHolder(view);
            }
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof DataViewHolder) {
                JsonData data = jsonData.get(position);
                DataViewHolder dataViewHolder = (DataViewHolder) holder;
                dataViewHolder.itemView.setTag(position);
                dataViewHolder.tvName.setText(data.name + "");
                dataViewHolder.tvDefination.setText(data.defination+"");
                dataViewHolder.tvView.setText(data.view+"");
                dataViewHolder.tvDate.setText(data.createDate+"");
            } else if (holder instanceof LoadingViewHolder) {
                LoadingViewHolder loadingViewHolder = (LoadingViewHolder) holder;
                loadingViewHolder.progressBar.setIndeterminate(true);
            }
        }

        @Override
        public int getItemCount() {
            return jsonData == null ? 0 : jsonData.size();
        }

        public void setLoaded() {
            isLoading = false;
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.category, menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==0){
            String s1=PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getString("langKey","ru");
            if(!s.equals(s1)){
                s=s1;
                finish();
                startActivity(getIntent());
            }
            byGovAgencies=PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getBoolean("govKey",true);
            byExternalUsers=PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getBoolean("outKey",true);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivityForResult(new Intent(CategoryActivity.this,SettingActivity.class),0);
            return true;
        }
        if(id==R.id.sort){
            AlertDialog.Builder adlog=new AlertDialog.Builder(CategoryActivity.this);
            language_name=new String[]{"Русский","Қазақша"};
            adlog.setSingleChoiceItems(language_name, languagePosition, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    languagePosition = which;
                    languageDialog.cancel();
                    if(isSelected)
                        setDataset();
                }
            });
            adlog.setTitle(R.string.language);
            languageDialog=adlog.create();
            languageDialog.show();

        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        String title=item.getTitle().toString();
        int p=0;
        if(item.getItemId()==R.id.nav_call){
            return true;
        }
        if(item.getItemId()==R.id.nav_mail){
            return true;
        }
        if(item.getItemId()==R.id.loaded_data){
            startActivity(new Intent(CategoryActivity.this,LoadedFileActivity.class));
            return true;
        }
        for(int i=0;i<titleList.length;i++)
            if(titleList[i].equals(title)){
                p=i;
                break;
            }
        linkPosition=p;
        isSelected=true;
        setDataset();
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void setDataset() {
        mRecyclerView.destroyDrawingCache();
        mRecyclerView.removeAllViews();
        mRecyclerView.setAdapter(null);
        page=2;
        isLast=false;
        new CustomAsynkTask().execute();
    }

    class CustomAsynkTask extends AsyncTask<String,String,String>{

        private String text;
        private Dialog dialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            AlertDialog.Builder adlog=new AlertDialog.Builder(CategoryActivity.this);
            adlog.setMessage(R.string.loading);
            adlog.setCancelable(false);
            dialog=adlog.create();
            dialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            try{
                text=httpLoader("page=1&count=10&byGovAgencies="+byGovAgencies+"&byExternalUsers="+byExternalUsers+"&sortBy="+sort_value[sortPosition]);
            }
            catch (Exception e){
                Log.d("Error",e.getMessage()+"");
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            setDataAdapter(text);
            try {
                dialog.cancel();
            }
            catch (Exception e){}
        }
    }

    private synchronized String httpLoader(String param)throws Exception{
        Log.d("LogD",param);
        HttpsURLConnection con=(HttpsURLConnection) new URL(linkList[linkPosition]).openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Accept-Language", "ru-RU,ru;q=0.5");
        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(param);
        wr.flush();
        wr.close();
        int responseCode = con.getResponseCode();
        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine="";
        Scanner a=new Scanner(in);
        while(a.hasNextLine())
            inputLine+=a.nextLine();

        //print result
        Log.d("LogD",inputLine);
        return inputLine;
    }
    interface OnLoadMoreListener {
        void onLoadMore();
    }
}
