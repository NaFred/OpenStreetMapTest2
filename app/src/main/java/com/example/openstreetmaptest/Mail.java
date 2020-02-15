package com.example.openstreetmaptest;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * @brief This class describes an AsyncTask to send an email
 */
public class Mail extends AsyncTask<Void,Void,Void>  {

    //Variables
    private Context mContext;
    private Session mSession;

    private String mEmail;
    private String mSubject;
    private String mMessage;

    private ProgressDialog mProgressDialog;

    /**
     * @brief Constructor for the Object Mail
     * @param mContext The app
     * @param mEmail The address
     * @param mSubject  The mail subject
     * @param mMessage  The message of the mail
     */
    public Mail(Context mContext, String mEmail, String mSubject, String mMessage) {
        this.mContext = mContext;
        this.mEmail = mEmail;
        this.mSubject = mSubject;
        this.mMessage = mMessage;
    }

    /**
     * @brief This function is executed before the Async Main Task is executed
     */
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        //Show progress dialog while sending email
        mProgressDialog = ProgressDialog.show(mContext,"Sending message", "Please wait...",false,false);
    }

    /**
     * @brief This function is executed after the main task of the Async Task is executed
     * @param aVoid
     */
    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        //Dismiss progress dialog when message successfully send
        mProgressDialog.dismiss();

        //Show success toast
        Toast.makeText(mContext,"Message Sent",Toast.LENGTH_SHORT).show();
    }

    /**
     * @brief This task is the Main AsyncTask
     * @param params
     * @return
     */
    @Override
    protected Void doInBackground(Void... params) {
        //Creating properties
        Properties props = new Properties();

        //Configuring properties for gmail
        props.put("mail.smtp.host", "smtp.web.de");
        props.put("mail.smtp.socketFactory.port", "587");
        //props.put("mail.smtp.socketFactory.class", "javax.net.tls.TLSSocketFactory");
        //props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "587");

        //Creating a new session
        mSession = Session.getDefaultInstance(props,
                new javax.mail.Authenticator() {
                    //Authenticating the password
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication("NaFred.eit@web.de", "hochschule"); //login information
                    }
                });

        try {
            //Creating MimeMessage object
            MimeMessage mm = new MimeMessage(mSession);

            //Setting sender address
            mm.setFrom(new InternetAddress("NaFred.eit@web.de"));
            //Adding receiver
            mm.addRecipient(Message.RecipientType.TO, new InternetAddress(mEmail));
            //Adding subject
            mm.setSubject(mSubject);
            //Adding message
            mm.setText(mMessage);
            //Sending email
            Transport.send(mm);

        } catch (MessagingException e) {
            e.printStackTrace();
        }
        return null;
    }
}