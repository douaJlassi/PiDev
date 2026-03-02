package Controllers;

import app.Session;
import entities.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import repositories.AgencyAnalyticsRepository;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

public class AgencyAnalyticsController {

    @FXML private javafx.scene.control.Label revenueLbl;
    @FXML private javafx.scene.control.Label bookingsLbl;
    @FXML private javafx.scene.control.Label approvalLbl;
    @FXML private javafx.scene.control.Label pendingLbl;

    @FXML private LineChart<String, Number> revenueChart;
    @FXML private PieChart statusPie;
    @FXML private BarChart<String, Number> topOffersChart;

    @FXML private TableView<AgencyTopClient> clientsTable;
    @FXML private TableColumn<AgencyTopClient, Integer> colClientId;
    @FXML private TableColumn<AgencyTopClient, Integer> colBookings;
    @FXML private TableColumn<AgencyTopClient, BigDecimal> colClientRevenue;

    private final AgencyAnalyticsRepository repo = new AgencyAnalyticsRepository();
    private final DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("MM-dd");

    @FXML
    public void initialize() {
        // table columns
        colClientId.setCellValueFactory(new PropertyValueFactory<>("idClient"));
        colBookings.setCellValueFactory(new PropertyValueFactory<>("bookings"));
        colClientRevenue.setCellValueFactory(new PropertyValueFactory<>("revenue"));

        loadAll();
    }

    @FXML
    private void onRefresh() {
        loadAll();
    }

    private void loadAll() {
        if (!Session.isAgency()) return;

        int idAgence = Session.getUserId();

        // KPI
        AgencyAnalyticsKpi k = repo.loadKpis(idAgence);
        revenueLbl.setText(k.getRevenue() + " TND");
        bookingsLbl.setText(String.valueOf(k.getConfirmedBookings()));
        approvalLbl.setText(String.format("%.1f%%", k.getApprovalRate()));
        pendingLbl.setText(String.valueOf(k.getPendingLines()));

        // Revenue line
        revenueChart.getData().clear();
        XYChart.Series<String, Number> s = new XYChart.Series<>();
        s.setName("Revenue");

        for (AgencyRevenuePoint p : repo.revenueLast30Days(idAgence)) {
            String label = p.getDay().format(dayFmt);
            s.getData().add(new XYChart.Data<>(label, p.getRevenue()));
        }
        revenueChart.getData().add(s);

        // Pie
        statusPie.setData(FXCollections.observableArrayList(repo.statusDistribution(idAgence)));

        // Top offers bar
        topOffersChart.getData().clear();
        XYChart.Series<String, Number> b = new XYChart.Series<>();
        b.setName("Revenue");
        for (AgencyTopOffer o : repo.topOffersByRevenue(idAgence, 8)) {
            b.getData().add(new XYChart.Data<>(shorten(o.getTitle(), 18), o.getRevenue()));
        }
        topOffersChart.getData().add(b);

        // Top clients table
        clientsTable.setItems(FXCollections.observableArrayList(repo.topClients(idAgence, 10)));
    }

    private String shorten(String s, int max) {
        if (s == null) return "-";
        s = s.trim();
        if (s.length() <= max) return s;
        return s.substring(0, Math.max(0, max - 1)) + "…";
    }
}