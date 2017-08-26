package app.com.smsreaddemo;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsMainActivity extends AppCompatActivity {

    private static final int READ_CONTACTS_REQ = 1;
    private static final String TAG = SmsMainActivity.class.getSimpleName();
    private TextView mTvExpenses;
    private TextView mTvDebited;
    private TextView mTvCredited;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_main);

        mTvCredited= (TextView) findViewById(R.id.tv_credited);
        mTvDebited= (TextView) findViewById(R.id.tv_debited);
        mTvExpenses= (TextView) findViewById(R.id.tv_expense);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if(ContextCompat.checkSelfPermission(SmsMainActivity.this, Manifest.permission.READ_SMS)
                    != PackageManager.PERMISSION_GRANTED){

                ActivityCompat.requestPermissions(SmsMainActivity.this,
                        new String[]{Manifest.permission.READ_SMS},READ_CONTACTS_REQ);
               /*
                if(ActivityCompat.shouldShowRequestPermissionRationale(SmsMainActivity.this,
                        Manifest.permission.READ_SMS)){
                    refreshSmsInbox();
            *//* do nothing*//*
                }*/

            }
            else{

               refreshSmsInbox();
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case READ_CONTACTS_REQ: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                        refreshSmsInbox();


                    // permission was granted, yay! do the
                    // calendar task you need to do.

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'switch' lines to check for other
            // permissions this app might request
        }
    }

    private double getCreditAmount(ArrayList<CreditMessage> creditMessages) {
        Pattern pattern=Pattern.compile("(?i)(?:(?:RS|INR|MRP)\\.?\\s?)(\\d+(:?\\,\\d+)?(\\,\\d+)?(\\.\\d{1,2})?)");
        Double creditAmount=0.00;

        for (int i=0;i<creditMessages.size();i++) {
            Matcher matcher = pattern.matcher(creditMessages.get(i).getMessageBody());
            if (matcher.find())
            {
                Log.e("amount_value= ", "" + matcher.group(0));
                String amount = (matcher.group(0).replaceAll("inr", ""));
                amount = amount.replaceAll("rs", "");
                amount = amount.replaceAll("Rs.", "");
                amount = amount.replaceAll("inr", "");
                amount = amount.replaceAll(" ", "");
                amount = amount.replaceAll(",", "");
                creditAmount=+Double.valueOf(amount);
            }
        }

        return creditAmount;
    }

    private double getDebitedAmount(ArrayList<DebitMessage> debitMessages)
    {
        Pattern pattern=Pattern.compile("(?i)(?:(?:RS|INR|MRP)\\.?\\s?)(\\d+(:?\\,\\d+)?(\\,\\d+)?(\\.\\d{1,2})?)");
        Double debitedAmount=0.00;

        for (int i=0;i<debitMessages.size();i++) {
            Matcher matcher = pattern.matcher(debitMessages.get(i).getMessageBody());
            if (matcher.find())
            {
                Log.e("amount_value= ", "" + matcher.group(0));
                String amount = (matcher.group(0).replaceAll("inr", ""));
                amount = amount.replaceAll("rs", "");
                amount = amount.replaceAll("Rs.", "");
                amount = amount.replaceAll("inr", "");
                amount = amount.replaceAll(" ", "");
                amount = amount.replaceAll(",", "");
                debitedAmount=+Double.valueOf(amount);
            }
        }

        return debitedAmount;
    }

    public void refreshSmsInbox() {

        Calendar c = Calendar.getInstance();

        c.add(Calendar.DAY_OF_YEAR, -30);

        long  MonthAgo = c.getTimeInMillis();

        ArrayList<CreditMessage> creditMessage=new ArrayList<>();
        ArrayList<DebitMessage> debitMessage=new ArrayList<>();
        ContentResolver contentResolver = getContentResolver();
        Cursor smsInboxCursor = contentResolver.query(Uri.parse("content://sms/inbox"), null,null , null, null);
        int messagecount=smsInboxCursor.getCount();
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);


            if (smsInboxCursor.moveToFirst()) {

                for (int i=0;i<messagecount;i++)
                {
                    Log.e(TAG,"READ SMS  "+smsInboxCursor.getString(smsInboxCursor.getColumnIndexOrThrow("body")));
                    if (smsInboxCursor.getString(smsInboxCursor.getColumnIndexOrThrow("body")).contains("credited")) {

                       String smsdate=smsInboxCursor.getString(smsInboxCursor.getColumnIndexOrThrow("date"));
                        if (Long.parseLong(smsdate)>MonthAgo) {
                            CreditMessage creditMess=new CreditMessage();
                            creditMess.setMessageBody(smsInboxCursor.getString(smsInboxCursor.getColumnIndex("body")));
                            creditMess.setTime(smsInboxCursor.getLong(smsInboxCursor.getColumnIndexOrThrow("date")));
                            creditMessage.add(creditMess);

                        }
                    } else if (smsInboxCursor.getString(smsInboxCursor.getColumnIndexOrThrow("body")).contains("debited")) {
                        String smsdate=smsInboxCursor.getString(smsInboxCursor.getColumnIndexOrThrow("date"));
                        if (Long.parseLong(smsdate)>MonthAgo) {
                            DebitMessage debitMess=new DebitMessage();
                            debitMess.setTime(smsInboxCursor.getLong(smsInboxCursor.getColumnIndexOrThrow("date")));
                            debitMess.setMessageBody(smsInboxCursor.getString(smsInboxCursor.getColumnIndex("body")));
                            debitMessage.add(debitMess);
                        }
                    }
                    smsInboxCursor.moveToNext();
                }

            }

        Log.v(TAG,"Credited Message Read :" +creditMessage.toString());
        Log.v(TAG,"Debited Message Read :" +debitMessage.toString());
        Double creditAmount= getCreditAmount(creditMessage);
        Double debitAmount =getDebitedAmount(debitMessage);

        mTvCredited.setText("Credited Amount :- " + roundString(String.valueOf(creditAmount),2));
        mTvDebited.setText("Debited Amount :- " + roundString(String.valueOf(debitAmount),2));
        mTvExpenses.setText("Expenses Percentage :- " + roundString(String.valueOf((debitAmount/creditAmount)*100),2));

        Log.v(TAG,"Credited Amount" + creditAmount);
        Log.v(TAG,"Debited Amount" + debitAmount);

    }

    public  String roundString(String Rval, int Rpl) {

        DecimalFormat df = null ;//("#.00");
        String finalStr = "";

        if(Rval.contains(".")){
            String [] inputArr = Rval.split("\\.");
            df = new DecimalFormat("#0.00");
            finalStr = df.format(Double.parseDouble(Rval));

            /*if(inputArr[1].length() == 1){
                inputArr[1] = inputArr[1] + "0";
            }else if(inputArr[1].length() >= 2){
                inputArr[1] = inputArr[1].substring(0,2);

            }
            finalStr = inputArr[0] +"."+ inputArr[1];*/

        }else{
            finalStr = Rval + ".00";
        }
        return finalStr;


    }



}
