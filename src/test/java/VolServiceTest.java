import projet.entites.vol;
import projet.services.VolService;
import org.junit.jupiter.api.*;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class VolServiceTest {
    static VolService volService;
    int idVol=4;
    @BeforeAll
    static void beforeAll() throws SQLException, ClassNotFoundException {
        volService = new VolService();
    }
    @Test
    @Order(1)
    void addVol() throws SQLException {
        java.util.Date utilDate = new java.util.Date();
        java.sql.Date sqlDate = new java.sql.Date(utilDate.getTime());
        vol v1=new vol(idVol,"testVol","description",105.2,false,120,"52","tunisie","japan",sqlDate,sqlDate);
        volService.insertOne(v1);
        List<vol> vols = volService.selectALL();
        assertTrue(vols.stream().anyMatch(vol -> vol.getId()== v1.getId()));

    }
    @Test
    @Order(2)
    void updateVol() throws SQLException {
        java.util.Date utilDate = new java.util.Date();
        java.sql.Date sqlDate = new java.sql.Date(utilDate.getTime());
        vol v1=new vol(idVol,"modifiedtestVol","description",105.2,false,120,"55","tunisie","japan",sqlDate,sqlDate);
        volService.updateOne(v1);
        List<vol> vols = volService.selectALL();
        assertTrue(vols.stream().anyMatch(vol -> vol.getNom().equals("modifiedtestVol") ));
    }
    @Test
    @Order(3)
    void deleteVol() throws SQLException {
        java.util.Date utilDate = new java.util.Date();
        java.sql.Date sqlDate = new java.sql.Date(utilDate.getTime());
        vol v1=new vol(idVol,"testVol","description",105.2,false,120,"52","tunisie","japan",sqlDate,sqlDate);
        volService.deleteOne(v1);
        List<vol> vols = volService.selectALL();
        assertFalse(vols.stream().anyMatch(vol -> vol.getId()==v1.getId()));
    }
}
