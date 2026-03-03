import org.junit.jupiter.api.*;
import java.sql.SQLException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import entities.Hotel;
import services.HotelService;
public class HotelServiceTest {
    static HotelService hotelService;

    @BeforeAll
    static void setUp() throws SQLException {
        hotelService = new HotelService();
    }
    @Test
    @Order(1)
    void addHotel() throws SQLException {
        Hotel h1=new Hotel("hotelTest","description",105.2,false,500,"hotel",5,"sousse","SINGLE");
        hotelService.insertOne(h1);
        List<Hotel> hotels = hotelService.selectALL();
        assertTrue(hotels.stream().anyMatch(hotel->hotel.getNom().equals(h1.getNom())));
    }
    @Test
    @Order(2)
    void updateHotel() throws SQLException {
        Hotel h2=new Hotel("hotelTestModified","description",105.2,false,500,"hotel",5,"sousse","SINGLE");
        hotelService.updateOne("hotelTest",h2);
        List<Hotel> hotels=hotelService.selectALL();
        assertTrue(hotels.stream().anyMatch(h->h.getNom().equals("hotelTestModified")));

    }
    @Test
    @Order(3)
    void deleteHotel() throws SQLException {
        Hotel h1=new Hotel("hotelTestModified","description",105.2,false,500,"hotel",5,"sousse","SINGLE");
        hotelService.deleteOne(h1);
        List<Hotel> hotels=hotelService.selectALL();
        assertFalse(hotels.stream().anyMatch(hotel->hotel.getNom().equals(h1.getNom())));
    }
}
