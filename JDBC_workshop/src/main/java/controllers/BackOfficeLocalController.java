//package controllers;
//
//import javafx.collections.FXCollections;
//import javafx.collections.ObservableList;
//import javafx.fxml.FXML;
//import javafx.scene.chart.BarChart;
//import javafx.scene.chart.LineChart;
//import javafx.scene.chart.PieChart;
//import javafx.scene.chart.XYChart;
//import javafx.scene.control.Label;
//import services.ServiceConversation;
//import services.ServiceMessage;
//
//import java.sql.SQLException;
//import java.util.Map;
//
//public class BackOfficeLocalController {
//    @FXML
//    private Label lblTotalConv;
//    @FXML private PieChart pieType, pieMedia;
//    @FXML private BarChart<String, Number> barGroupSize;
//    @FXML private LineChart<String, Number> lineActivity;
//
//    private ServiceConversation serConv = new ServiceConversation();
//    private ServiceMessage serMsg = new ServiceMessage();
//
//    @FXML
//    public void initialize() {
//        try {
//            setupPieType();
//            setupPieMedia();
//            setupBarGroupSize();
//            setupLineActivity();
//        } catch (SQLException e) { e.printStackTrace(); }
//    }
//
//    private void setupPieType() throws SQLException {
//        Map<String, Integer> data = serConv.getStatsByType();
//        int total = data.values().stream().mapToInt(Integer::intValue).sum();
//        lblTotalConv.setText(String.valueOf(total));
//
//        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
//        data.forEach((k, v) -> pieData.add(new PieChart.Data(k, v)));
//        pieType.setData(pieData);
//    }
//
//    private void setupBarGroupSize() throws SQLException {
//        XYChart.Series<String, Number> series = new XYChart.Series<>();
//        series.setName("Members");
//        serConv.getGroupSizeStats().forEach((titre, count) -> {
//            series.getData().add(new XYChart.Data<>(titre, count));
//        });
//        barGroupSize.getData().add(series);
//    }
//
//    private void setupPieMedia() throws SQLException {
//        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
//        serMsg.getMediaTypeStats().forEach((k, v) -> pieData.add(new PieChart.Data(k, v)));
//        pieMedia.setData(pieData);
//    }
//
//    private void setupLineActivity() throws SQLException {
//        XYChart.Series<String, Number> series = new XYChart.Series<>();
//        series.setName("Messages per day");
//        serMsg.getDailyActivityStats().forEach((date, count) -> {
//            series.getData().add(new XYChart.Data<>(date, count));
//        });
//        lineActivity.getData().add(series);
//    }
//}