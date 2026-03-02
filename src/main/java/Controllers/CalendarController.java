package Controllers;

import services.ActiviteService;
import gestion_activite.Activite;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.RowConstraints;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class CalendarController {

    @FXML private GridPane dayNamesGrid;
    @FXML private GridPane calendarGrid;
    @FXML private Label lblMonthYear;
    @FXML private Label infoLabel;
    @FXML private Button btnPrevMonth;
    @FXML private Button btnNextMonth;

    private int guideId;

    private List<Activite> allActivities;
    private List<LocalDate> activityDates;
    private YearMonth currentMonth;
    private LocalDate selectedDate;

    // French day names starting Monday
    private static final String[] DAY_NAMES = {"LUN", "MAR", "MER", "JEU", "VEN", "SAM", "DIM"};

    @FXML
    public void initialize() {
        currentMonth = YearMonth.now();
        setupColumnConstraints();
        buildDayNamesRow();
    }

    public void setGuideId(int guideId) {
        this.guideId = guideId;
        loadActivityDates();
    }

    private void loadActivityDates() {
        ActiviteService service = new ActiviteService();
        try {
            allActivities = service.selectByGuide(guideId);
            List<Activite> activites = service.selectByGuide(guideId);
            activityDates = activites.stream()
                    .map(a -> a.getDateActivite().toLocalDateTime().toLocalDate())
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            activityDates = List.of();
            e.printStackTrace();
        }
        buildCalendar();
    }

    private void setupColumnConstraints() {
        for (GridPane grid : new GridPane[]{dayNamesGrid, calendarGrid}) {
            grid.getColumnConstraints().clear();
            for (int i = 0; i < 7; i++) {
                ColumnConstraints col = new ColumnConstraints();
                col.setMinWidth(52);
                col.setPrefWidth(52);
                col.setMaxWidth(52);
                grid.getColumnConstraints().add(col);
            }
        }
    }

    private void buildDayNamesRow() {
        dayNamesGrid.getChildren().clear();
        for (int i = 0; i < 7; i++) {
            Label lbl = new Label(DAY_NAMES[i]);
            lbl.getStyleClass().add("day-name");
            dayNamesGrid.add(lbl, i, 0);
        }
    }

    private void buildCalendar() {
        calendarGrid.getChildren().clear();
        calendarGrid.getRowConstraints().clear();

        // Update month/year label
        String monthName = currentMonth.getMonth()
                .getDisplayName(TextStyle.FULL, Locale.FRENCH);
        monthName = monthName.substring(0, 1).toUpperCase() + monthName.substring(1);
        lblMonthYear.setText(monthName + " " + currentMonth.getYear());

        LocalDate firstDay = currentMonth.atDay(1);
        // Monday = 1, so offset: Monday=0, Tue=1... Sun=6
        int startOffset = firstDay.getDayOfWeek().getValue() - 1;
        int daysInMonth = currentMonth.lengthOfMonth();
        int totalCells = startOffset + daysInMonth;
        int rows = (int) Math.ceil(totalCells / 7.0);

        for (int r = 0; r < rows; r++) {
            RowConstraints rc = new RowConstraints(44);
            calendarGrid.getRowConstraints().add(rc);
        }

        LocalDate today = LocalDate.now();

        for (int cell = 0; cell < rows * 7; cell++) {
            int col = cell % 7;
            int row = cell / 7;
            int dayNum = cell - startOffset + 1;

            LocalDate cellDate;
            boolean isCurrentMonth = dayNum >= 1 && dayNum <= daysInMonth;

            if (dayNum < 1) {
                // Previous month fill
                cellDate = firstDay.minusDays(startOffset - cell);
            } else if (dayNum > daysInMonth) {
                cellDate = firstDay.plusDays(dayNum - 1);
            } else {
                cellDate = currentMonth.atDay(dayNum);
            }

            Button btn = new Button(String.valueOf(cellDate.getDayOfMonth()));
            btn.getStyleClass().add("day-cell");

            if (!isCurrentMonth) {
                btn.getStyleClass().add("day-cell-other");
            } else {
                boolean hasActivity = activityDates != null && activityDates.contains(cellDate);
                boolean isToday = cellDate.equals(today);
                boolean isSelected = cellDate.equals(selectedDate);

                if (isSelected) {
                    btn.getStyleClass().add("day-cell-selected");
                } else if (hasActivity) {
                    btn.getStyleClass().add("day-cell-active");
                }
                if (isToday) {
                    btn.getStyleClass().add("day-cell-today");
                }
            }

            final LocalDate finalDate = cellDate;
            final boolean finalIsCurrentMonth = isCurrentMonth;
            btn.setOnAction(e -> handleDayClick(finalDate, finalIsCurrentMonth));

            calendarGrid.add(btn, col, row);
        }
    }

    private void handleDayClick(LocalDate date, boolean isCurrentMonth) {
        selectedDate = date;

        if (!isCurrentMonth) {
            currentMonth = YearMonth.from(date);
        }

        // Filtrer les activités de cette date
        List<Activite> activitiesOnDate = allActivities.stream()
                .filter(a -> a.getDateActivite().toLocalDateTime().toLocalDate().equals(date))
                .collect(Collectors.toList());

        if (!activitiesOnDate.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Activite a : activitiesOnDate) {
                if (sb.length() > 0) sb.append("\n");
                sb.append(a.getTitre()).append(" – ").append(a.getLieu());
            }
            infoLabel.setText(sb.toString());
            infoLabel.getStyleClass().remove("info-label-active");
            infoLabel.getStyleClass().add("info-label-active");
        } else {
            infoLabel.setText("Aucune activité le " + date.getDayOfMonth() + " " +
                    date.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH));
            infoLabel.getStyleClass().remove("info-label-active");
        }

        buildCalendar();
    }

    @FXML
    private void handlePrevMonth() {
        currentMonth = currentMonth.minusMonths(1);
        buildCalendar();
    }

    @FXML
    private void handleNextMonth() {
        currentMonth = currentMonth.plusMonths(1);
        buildCalendar();
    }
}