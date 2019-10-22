package com.sandra.falldetector2.service;

import java.util.Date;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.sandra.falldetector2.App;
import com.sandra.falldetector2.model.Contact;

public class MailService
{
    private String mToList;
    private String mFromList;
    private String mSubject;
    private String mMessageText;
    final private static String mHost = "smtp.gmail.com";

    private Properties mProperties;

    /**
     * SimpleAuthenticator is used to do simple authentication when the SMTP
     * server requires it.
     */

    private static class SMTPAuthenticator extends javax.mail.Authenticator {

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {

            String username = "tech.lucasfeitosa.falldetector@gmail.com";//*DataService.getSetting(DataService.SETTING_SMTP_USER)*//*;
            String password = "fallD123"; //*DataService.getSetting(DataService.SETTING_SMTP_PASSWORD)*//*;

            return new PasswordAuthentication(username, password);
        }
    }

    public MailService(String fromList,String toList, String subject, String messageText )
    {
        mToList = toList;
        mFromList = fromList;
        mSubject = subject;
        mMessageText = messageText;
        //mHost = "smtp.gmail.com";
    }
    public boolean sendMessage(StringBuilder returnMessage)
    {
        mProperties = new Properties();
        mProperties.put("mail.smtp.host", mHost);
        mProperties.put("mail.smtp.port", "465");  // gmail smtp port
        mProperties.put("mail.smtp.auth", "true");

        mProperties.put("mail.smtp.socketFactory.port", "465");
        mProperties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        mProperties.put("mail.smtp.socketFactory.fallback", "false");

        Authenticator auth = new SMTPAuthenticator();
        Session session = Session.getInstance(mProperties,auth);
        //Session session = Session.getInstance(mProperties);

        try
        {
            //create a message
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(mFromList));
            InternetAddress[] address = {new InternetAddress(mToList)};
            Contact contact[] = App.getInstance().getContactRepository().getAllContacts();
            for (Contact c:contact){
                //message.addRecipient(Message.RecipientType.TO,new InternetAddress(c.getMail()));
            }
            //message.setRecipients(Message.RecipientType.TO, address);
            message.setSubject(mSubject);
            message.setSentDate(new Date());

            message.setText(mMessageText);

            Transport.send(message);
            returnMessage.append("Order placed successfully.");
            return true;
        }
        catch (MessagingException mEx)
        {
            mEx.printStackTrace();
            returnMessage.append("Inside Exception");
            String res = mEx.getMessage();
            returnMessage.append(res);
            //System.out.println();
            Exception ex = mEx;
            do {
                if (ex instanceof SendFailedException) {
                    SendFailedException sfex = (SendFailedException)ex;
                    //returnMessage.append(sfex.getMessage().toString());
                    Address[] invalid = sfex.getInvalidAddresses();
                    if (invalid != null) {
                        returnMessage.append("    ** Invalid Addresses");
                        for (int i = 0; i < invalid.length; i++)
                            returnMessage.append("         " + invalid[i]);
                    }
                    Address[] validUnsent = sfex.getValidUnsentAddresses();
                    if (validUnsent != null) {
                        returnMessage.append("    ** ValidUnsent Addresses");
                        for (int i = 0; i < validUnsent.length; i++)
                            returnMessage.append("         " + validUnsent[i]);
                    }
                    Address[] validSent = sfex.getValidSentAddresses();
                    if (validSent != null) {
                        returnMessage.append("    ** ValidSent Addresses");
                        for (int i = 0; i < validSent.length; i++)
                            returnMessage.append("         "+validSent[i]);
                    }
                }

                if (ex instanceof MessagingException) {
                    ex = ((MessagingException) ex).getNextException();
                    //returnMessage.append(ex.getMessage().toString());
                }
                else
                    ex = null;
            } while (ex != null);
            return false;
        }
    }

}