import services.ServiceService;
import entites.service;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import java.sql.SQLException;
import java.util.List;

public class ServiceServicesTest {
    static ServiceService Service;

    @BeforeAll
    static void setUp() throws SQLException {
        Service = new ServiceService();
    }
    @Test
    @Order(1)
    void addService() throws SQLException {
        service h1=new service("ServiceTest","description",105.2,false,500,"none");
        Service.insertOne(h1);
        List<service> Services = Service.selectALL();
        assertTrue(Services.stream().anyMatch(service->service.getNom().equals(h1.getNom())));
    }
    @Test
    @Order(2)
    void updateService() throws SQLException {
        service h2=new service("ServiceTestModified","description",105.2,false,500,"none");
        Service.updateOne("ServiceTest",h2);
        List<service> services=Service.selectALL();
        assertTrue(services.stream().anyMatch(s->s.getNom().equals("hotelTestModified")));

    }
    @Test
    @Order(4)
    void deleteService() throws SQLException {
        service h1=new service("ServiceTestModified","description",105.2,false,500,"none");
        Service.deleteOne(h1);
        List<service> services=Service.selectALL();
        assertFalse(services.stream().anyMatch(service->service.getNom().equals(h1.getNom())));
    }
    @Test
    @Order(3)
    void getID() throws SQLException {
        service h1=new service("ServiceTestModified","description",105.2,false,500,"none");
        Service.getId(h1.getNom());
    }
}
