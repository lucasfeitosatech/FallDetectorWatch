package tech.lucasfeitosa.falldetector2.repository;

import io.realm.Realm;
import io.realm.RealmResults;
import tech.lucasfeitosa.falldetector2.model.Contact;

public class ContactRepository {

    public void saveContact(Contact c){
        Realm.getDefaultInstance().executeTransaction(realm -> {
            realm.copyToRealm(c);
        });
    }

    public Contact[] getAllContacts(){
        Object[] objects = Realm.getDefaultInstance().where(Contact.class).findAll().toArray();
        Contact[] contacts = Realm.getDefaultInstance().where(Contact.class).findAll().toArray(new Contact[objects.length]);
        return contacts;
    }

    public void removeContact(Contact contact){
        Realm realm = Realm.getDefaultInstance();
        final RealmResults<Contact> contacts = realm.where(Contact.class).findAll();
        Contact c = contacts.where().equalTo("number",contact.getNumber()).equalTo("name",contact.getName()).findFirst();
        if(c!=null){
            if (!realm.isInTransaction())
            {
                realm.beginTransaction();
            }
            c.deleteFromRealm();
            realm.commitTransaction();
        }
    }

    public void updateContact(Contact contact){
        Realm realm = Realm.getDefaultInstance();
        final RealmResults<Contact> contacts = realm.where(Contact.class).findAll();
        Object[] objects = Realm.getDefaultInstance().where(Contact.class).findAll().toArray();
        Contact[] contactsArr = Realm.getDefaultInstance().where(Contact.class).findAll().toArray(new Contact[objects.length]);
        for (Contact c:contactsArr){
            if (!realm.isInTransaction())
            {
                realm.beginTransaction();
            }
            c.setImportant(false);
            realm.commitTransaction();
        }
        Contact c = contacts.where().equalTo("number",contact.getNumber()).equalTo("name",contact.getName()).findFirst();
        if(c!=null){
            if (!realm.isInTransaction())
            {
                realm.beginTransaction();
            }
            c.setImportant(true);
            realm.commitTransaction();
        }
    }
}