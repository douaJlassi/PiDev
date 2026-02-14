package projet.test;

import projet.entites.Hotel;
import projet.services.HotelService;
import projet.utils.MyDBConnexion;

import  projet.entites.vol;
import projet.services.VolService;

import java.sql.SQLException;
import java.sql.Time;
import java.util.Date;
import java.util.List;

public class Test {
    public static void main(String[] args) throws SQLException {
        MyDBConnexion c1 = MyDBConnexion.getInstance();
        java.util.Date utilDate = new java.util.Date();
        java.sql.Date sqlDate = new java.sql.Date(utilDate.getTime());
        System.out.println(sqlDate);
        vol v1=new vol(3,"vol444","description",105.2,false,120,"52","tunisie","japan",sqlDate,sqlDate);
        Hotel h1=new Hotel(6,"hotel2","description",105.2,false,500,5,"sousse","SINGLE");
        VolService vs = new VolService();
        HotelService hs = new HotelService();
         //vs.insertOne(v1);
         //vs.updateOne(v1);
         //System.out.println(vs.selectALL());
        //vs.deleteOne(v1);
        //hs.insertOne(h1);
        // hs.updateOne(h1);
       // hs.deleteOne(h1);
        //System.out.println(hs.selectALL());
}
}
