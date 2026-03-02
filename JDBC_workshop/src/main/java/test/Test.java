package test;

import entities.Person;
import services.PersonService;
import utils.MyDBConnexion;

import java.sql.SQLException;

public class Test {

    public static void main(String[] args) {
        MyDBConnexion c1 = MyDBConnexion.getInstance();

        Person p = new  Person( 4,"Wassimm15", "Bech ye5ou - 5","www@www.com","11zdzd", java.sql.Date.valueOf("2025-01-01"),"x", "0.1");

        PersonService ps = new PersonService() ;

        try {
            ps.insertOneUpdated(p);
            //ps.updateOne(p);
            //ps.deleteOne(p);

            System.out.println(ps.selectALL());
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }

    }
}
