import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.time.*;
import java.time.format.*;

public class TicketBookingSystem extends JFrame {
    private static final int INFINITY = Integer.MAX_VALUE;
    private static final int NUM_STATIONS = 6;
    private static final int TRAIN_SPEED = 30; // km/h
    private static final int STATION_WAIT_TIME = 10; // minutes
    private static final int MIN_TRANSFER_TIME = 5; // minutes
    private static final LocalTime FIRST_TRAIN = LocalTime.of(6, 0);
    private static final LocalTime LAST_TRAIN = LocalTime.of(20, 0);
    private static final int TRAIN_INTERVAL = 10; // minutes between trains
    private static final int SEARCH_WINDOW = 30; // minutes to search before and after desired time
    
    private JComboBox<String> startStationCombo;
    private JComboBox<String> endStationCombo;
    private JSpinner timeSpinner;
    private JTextArea resultArea;
    private JButton findPathButton;
    private JButton confirmButton;
    private JPanel trainSelectionPanel;
    private ArrayList<LocalTime[]> selectedTimes;
    private ArrayList<Integer> currentPath;
    
    private final int[][] graph = new int[NUM_STATIONS][NUM_STATIONS];
    private final String[] stationNames = {"A", "B", "C", "D", "E", "F"};
    
    public TicketBookingSystem() {
        initializeGraph();
        setupGUI();
        selectedTimes = new ArrayList<>();
    }
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    
    private void initializeGraph() {
        for (int i = 0; i < NUM_STATIONS; i++) {
            Arrays.fill(graph[i], INFINITY);
            graph[i][i] = 0;
        }
        
        // Add connections from the given matrix
        addConnection(0, 1, 10); // A-B
        addConnection(0, 2, 22); // A-C
        addConnection(0, 4, 8);  // A-E
        addConnection(1, 2, 15); // B-C
        addConnection(1, 3, 9);  // B-D
        addConnection(1, 5, 7);  // B-F
        addConnection(2, 3, 9);  // C-D
        addConnection(3, 4, 5);  // D-E
        addConnection(3, 5, 12); // D-F
        addConnection(4, 5, 16); // E-F
    }
    
    private void addConnection(int from, int to, int distance) {
        graph[from][to] = distance;
        graph[to][from] = distance; // Undirected graph
    }
    
    private void setupGUI() {
        setTitle("Metro Ticket Booking System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        
        // Input Panel
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        GridBagConstraints gridBagLayout = new GridBagConstraints();
        gridBagLayout.fill = GridBagConstraints.HORIZONTAL;
        gridBagLayout.insets = new Insets(5, 5, 5, 5);
        
        startStationCombo = new JComboBox<>(stationNames);
        endStationCombo = new JComboBox<>(stationNames);
        
        SpinnerDateModel timeModel = new SpinnerDateModel();
        timeSpinner = new JSpinner(timeModel);
        JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(timeSpinner, "HH:mm");
        timeSpinner.setEditor(timeEditor);
        
        gridBagLayout.gridx = 0; gridBagLayout.gridy = 0;
        inputPanel.add(new JLabel("Start Station:"), gridBagLayout);
        
        gridBagLayout.gridx = 1;
        inputPanel.add(startStationCombo, gridBagLayout);
        
        gridBagLayout.gridx = 0; gridBagLayout.gridy = 1;
        inputPanel.add(new JLabel("End Station:"), gridBagLayout);
        
        gridBagLayout.gridx = 1;
        inputPanel.add(endStationCombo, gridBagLayout);
        
        gridBagLayout.gridx = 0; gridBagLayout.gridy = 2;
        inputPanel.add(new JLabel("Desired Departure Time:"), gridBagLayout);
        
        gridBagLayout.gridx = 1;
        inputPanel.add(timeSpinner, gridBagLayout);
        
        findPathButton = new JButton("Find Available Trains");
        findPathButton.addActionListener(e -> findPath());
        
        gridBagLayout.gridx = 0; gridBagLayout.gridy = 3;
        gridBagLayout.gridwidth = 2;
        inputPanel.add(findPathButton, gridBagLayout);
        
        confirmButton = new JButton("Confirm Booking");
        confirmButton.setEnabled(false);
        confirmButton.setFont(new Font("Arial", Font.BOLD, 14));
        confirmButton.setPreferredSize(new Dimension(120, 35));
        confirmButton.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        confirmButton.addActionListener(e -> showTicket());
        
        // Train Selection Panel
        trainSelectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        trainSelectionPanel.setBorder(BorderFactory.createTitledBorder("Available Trains"));
        JScrollPane trainScrollPane = new JScrollPane(trainSelectionPanel);
        trainScrollPane.setPreferredSize(new Dimension(750, 200));
        
        // Result Area
        resultArea = new JTextArea(15, 40);
        resultArea.setEditable(false);
        JScrollPane resultScrollPane = new JScrollPane(resultArea);
        
        // Main Panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(trainScrollPane, BorderLayout.NORTH);
        mainPanel.add(resultScrollPane, BorderLayout.CENTER);
        mainPanel.add(confirmButton, BorderLayout.SOUTH);

        add(inputPanel, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(800, 600));
    }
    
    private ArrayList<LocalTime[]> getAvailableTrains(LocalTime currentTime, int travelMinutes) {
        ArrayList<LocalTime[]> trains = new ArrayList<>();
        LocalTime systemTime = LocalTime.now(); // Get current system time

        LocalTime startWindow = currentTime.minusMinutes(SEARCH_WINDOW);
        LocalTime endWindow = currentTime.plusMinutes(SEARCH_WINDOW);

        if (startWindow.isBefore(systemTime)) {
            startWindow = systemTime;
        }

        // Start with the first possible train time
        LocalTime nextTrainTime = startWindow;
        int minutes = nextTrainTime.getMinute();
        int roundedMinutes = ((minutes + TRAIN_INTERVAL - 1) / TRAIN_INTERVAL) * TRAIN_INTERVAL;

        if (roundedMinutes == 60) {
            nextTrainTime = nextTrainTime.plusHours(1).withMinute(0);
        } else {
            nextTrainTime = nextTrainTime.withMinute(roundedMinutes);
        }
        nextTrainTime = nextTrainTime.withSecond(0).withNano(0);

        int optionsCount = 0;
        while (!nextTrainTime.isAfter(endWindow) && optionsCount < 5) {
            // Check if train has not departed yet
            if (!nextTrainTime.isBefore(systemTime) && 
                !nextTrainTime.isBefore(FIRST_TRAIN) && 
                !nextTrainTime.isAfter(LAST_TRAIN)) {

                LocalTime arrival = nextTrainTime.plusMinutes(travelMinutes + STATION_WAIT_TIME);
                if (!arrival.isAfter(LAST_TRAIN)) {
                    trains.add(new LocalTime[]{nextTrainTime, arrival});
                    optionsCount++;
                }
            }
            nextTrainTime = nextTrainTime.plusMinutes(TRAIN_INTERVAL);
        }

        trains.sort((a, b) -> a[0].compareTo(b[0]));
        return trains;
}
    
    private void findPath() {
        trainSelectionPanel.removeAll();
        selectedTimes.clear();
        confirmButton.setEnabled(false);

        int startStation = startStationCombo.getSelectedIndex();
        int endStation = endStationCombo.getSelectedIndex();

        if (startStation == endStation) {
            JOptionPane.showMessageDialog(this, "Please select different stations for start and end points.");
            return;
        }
        
        // Get selected time
        Date selectedDate = (Date) timeSpinner.getValue();
        LocalTime selectedTime = LocalTime.ofInstant(selectedDate.toInstant(), 
                                                   java.time.ZoneId.systemDefault())
                                        .withSecond(0)
                                        .withNano(0);
        LocalTime currentTime = LocalTime.now();
        
        if (selectedTime.isBefore(currentTime)) {
            JOptionPane.showMessageDialog(this, 
                "Please set a proper depature time considering your checking-in time.");
            return;
        }
        
        if (selectedTime.isBefore(FIRST_TRAIN) || selectedTime.isAfter(LAST_TRAIN)) {
            JOptionPane.showMessageDialog(this, 
                "Trains operate only between " + 
                FIRST_TRAIN.format(TIME_FORMATTER) + " and " + 
                LAST_TRAIN.format(TIME_FORMATTER) + ".");
            return;
        }
        
        // To find shortest path
        int[] distances = new int[NUM_STATIONS];
        int[] previousStations = new int[NUM_STATIONS];
        dijkstra(startStation, distances, previousStations);
        
        
        // Get routes
        currentPath = reconstructPath(previousStations, startStation, endStation);
        
        if (currentPath.isEmpty()) {
            resultArea.setText("No route available between selected stations.");
            return;
        }
        
        // Get available trains
        generateTrainOptions(currentPath, selectedTime);
        
        trainSelectionPanel.revalidate();
        trainSelectionPanel.repaint();
    }
    
    private void generateTrainOptions(ArrayList<Integer> path, LocalTime desiredTime) {
        trainSelectionPanel.removeAll();
        selectedTimes.clear();
        confirmButton.setEnabled(false);

        JPanel segmentsPanel = new JPanel();
        segmentsPanel.setLayout(new BoxLayout(segmentsPanel, BoxLayout.X_AXIS));

        LocalTime currentTime = desiredTime;

        for (int i = 0; i < path.size() - 1; i++) {
            int from = path.get(i);
            int to = path.get(i + 1);
            int distance = graph[from][to];

            int travelMinutes = (int) Math.ceil((distance / (double) TRAIN_SPEED) * 60);
            ArrayList<LocalTime[]> availableTrains = getAvailableTrains(currentTime, travelMinutes);

            if (availableTrains.isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "No available trains found for segment " + stationNames[from] + " to " + stationNames[to]);
                return;
            }

            JPanel segmentPanel = new JPanel();
            segmentPanel.setLayout(new BoxLayout(segmentPanel, BoxLayout.Y_AXIS));
            segmentPanel.setBorder(BorderFactory.createTitledBorder(
                String.format("%s to %s", stationNames[from], stationNames[to])));

            ButtonGroup group = new ButtonGroup();
            final int segmentIndex = i;

            for (LocalTime[] trainTimes : availableTrains) {
                JRadioButton trainOption = new JRadioButton(String.format(
                    "%s - %s", 
                    trainTimes[0].format(DateTimeFormatter.ofPattern("HH:mm")),
                    trainTimes[1].format(DateTimeFormatter.ofPattern("HH:mm"))));

                trainOption.addActionListener(e -> {
                    while (selectedTimes.size() > segmentIndex) {
                        selectedTimes.remove(selectedTimes.size() - 1);
                    }
                    selectedTimes.add(trainTimes);
                    updateSchedule(path);
                    validateSelection();

                    if (segmentIndex < path.size() - 2) {
                        LocalTime nextSegmentStartTime = trainTimes[1].plusMinutes(MIN_TRANSFER_TIME);
                        regenerateNextSegment(path, segmentIndex + 1, nextSegmentStartTime);
                    }
                });

                group.add(trainOption);
                segmentPanel.add(trainOption);
            }

            segmentPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(5, 5, 5, 5),
                segmentPanel.getBorder()));

            segmentsPanel.add(segmentPanel);
            if (i < path.size() - 2) {
                segmentsPanel.add(Box.createRigidArea(new Dimension(10, 0)));
            }
        }

        trainSelectionPanel.add(segmentsPanel);
        updateSchedule(path); // Clear the display initially
    }
    
     private void regenerateNextSegment(ArrayList<Integer> path, int segmentIndex, LocalTime startTime) {
        int from = path.get(segmentIndex);
        int to = path.get(segmentIndex + 1);
        int distance = graph[from][to];
        
        int travelMinutes = (int) Math.ceil((distance / (double) TRAIN_SPEED) * 60);
        ArrayList<LocalTime[]> availableTrains = getAvailableTrains(startTime, travelMinutes);
        
        // Remove impossible train connections
        availableTrains.removeIf(train -> train[0].isBefore(startTime.plusMinutes(MIN_TRANSFER_TIME)));
        
        // Update the UI to accomodate the next train times
        Component[] components = trainSelectionPanel.getComponents();
        if (components.length > 0 && components[0] instanceof JPanel) {
            JPanel segmentsPanel = (JPanel) components[0];
            Component[] segmentPanels = segmentsPanel.getComponents();
            
            if (segmentIndex < segmentPanels.length) {
                JPanel nextSegmentPanel = (JPanel) segmentPanels[segmentIndex * 2];
                nextSegmentPanel.removeAll();
                
                ButtonGroup group = new ButtonGroup();
                for (LocalTime[] trainTimes : availableTrains) {
                    JRadioButton trainOption = new JRadioButton(String.format(
                        "%s - %s", 
                        trainTimes[0].format(DateTimeFormatter.ofPattern("HH:mm")),
                        trainTimes[1].format(DateTimeFormatter.ofPattern("HH:mm"))));
                        
                    trainOption.addActionListener(e -> {
                        while (selectedTimes.size() > segmentIndex) {
                            selectedTimes.remove(selectedTimes.size() - 1);
                        }
                        selectedTimes.add(trainTimes);
                        updateSchedule(path);
                        validateSelection();
                        
                        if (segmentIndex < path.size() - 2) {
                            LocalTime nextSegmentStartTime = trainTimes[1].plusMinutes(MIN_TRANSFER_TIME);
                            regenerateNextSegment(path, segmentIndex + 1, nextSegmentStartTime);
                        }
                    });
                    
                    group.add(trainOption);
                    nextSegmentPanel.add(trainOption);
                }
                
                nextSegmentPanel.revalidate();
                nextSegmentPanel.repaint();
            }
        }
    }
    
    private void validateSelection() {
        // Check if all segments have selected trains and times are valid
        if (selectedTimes.size() == currentPath.size() - 1) {
            confirmButton.setEnabled(true);
            updateSchedule(currentPath);
        } else {
            confirmButton.setEnabled(false);
        }
    }
    
    private void showTicket() {
        if (selectedTimes.isEmpty() || currentPath.isEmpty()) {
            return;
        }

        StringBuilder ticket = new StringBuilder();
        ticket.append("╔══════════════════════════════════════════════════════════════╗\n");
        ticket.append("                      METRO TICKET                      \n");
        ticket.append("╠══════════════════════════════════════════════════════════════╣\n");
        ticket.append(String.format("  From: %-52s  \n", "Station " + stationNames[currentPath.get(0)]));
        ticket.append(String.format("  To:   %-52s  \n", "Station " + stationNames[currentPath.get(currentPath.size() - 1)]));
        ticket.append(String.format("  Date: %-52s  \n", java.time.LocalDate.now()));
        ticket.append("\n");
        ticket.append("                     JOURNEY DETAILS                    \n");
        ticket.append("╠══════════════════════════════════════════════════════════════╣\n");

        int totalMinutes = 0;
        LocalTime previousArrival = null;
        boolean hasTightConnection = false;

        for (int i = 0; i < selectedTimes.size(); i++) {
            LocalTime[] times = selectedTimes.get(i);
            int from = currentPath.get(i);
            int to = currentPath.get(i + 1);

            if (previousArrival != null) {
                int transferTime = (int) previousArrival.until(times[0], java.time.temporal.ChronoUnit.MINUTES);
                ticket.append("                                                              \n");
                ticket.append(String.format("  Transfer at Station %-41s  \n", stationNames[from]));
                ticket.append(String.format("  Wait time: %-47s  \n", transferTime + " minutes"));
                if (transferTime < MIN_TRANSFER_TIME) {
                    ticket.append("  ⚠ WARNING: This is a tight connection!                      \n");
                    hasTightConnection = true;
                }
                ticket.append("                                                              \n");
                totalMinutes += transferTime;
            }

            int journeyMinutes = (int) times[0].until(times[1], java.time.temporal.ChronoUnit.MINUTES);
            ticket.append(String.format("  Train %-53d  \n", i + 1));
            ticket.append(String.format("  %-56s  \n", stationNames[from] + " → " + stationNames[to]));
            ticket.append(String.format("  Departure: %-48s  \n", times[0].format(DateTimeFormatter.ofPattern("HH:mm")) + " hrs"));
            ticket.append(String.format("  Arrival:   %-48s  \n", times[1].format(DateTimeFormatter.ofPattern("HH:mm")) + " hrs"));
            ticket.append(String.format("  Duration:  %-48s  \n", journeyMinutes + " minutes"));
            ticket.append("╟──────────────────────────────────────────────────────────────╢\n");

            totalMinutes += journeyMinutes;
            previousArrival = times[1];
        }

        ticket.append(String.format("  Total Journey Time: %-42s  \n", totalMinutes + " minutes"));
        ticket.append("╟──────────────────────────────────────────────────────────────╢\n");
        ticket.append("                     IMPORTANT NOTES                     \n");
        ticket.append("╠══════════════════════════════════════════════════════════════╣\n");
        if (hasTightConnection) {
            ticket.append("  ⚠ WARNING: This journey includes tight connections!          \n");
            ticket.append("  Please be prepared to move quickly between trains.           \n");
        }
        ticket.append("  • Please arrive 5 minutes before departure\n");
        ticket.append("  • Keep this ticket until the end of your journey\n");
        ticket.append("  • Follow station staff instructions at all times\n");
        ticket.append("╚══════════════════════════════════════════════════════════════╝\n");

        JTextArea ticketArea = new JTextArea(ticket.toString());
        ticketArea.setEditable(false);
        ticketArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(ticketArea);
        scrollPane.setPreferredSize(new Dimension(600, 600));

        JDialog ticketDialog = new JDialog(this, "Your Ticket", true);
        ticketDialog.add(scrollPane);
        ticketDialog.pack();
        ticketDialog.setLocationRelativeTo(this);
        ticketDialog.setVisible(true);
    }
    
     private void updateSchedule(ArrayList<Integer> path) {
        StringBuilder schedule = new StringBuilder();

        if (!selectedTimes.isEmpty()) {
            schedule.append(String.format("Trip %s to %s\n", 
                    stationNames[path.get(0)], stationNames[path.get(path.size() - 1)]));
            schedule.append("--------------\n\n");

            int totalMinutes = 0;
            LocalTime previousArrival = null;
            boolean hasTightConnection = false;

            for (int i = 0; i < selectedTimes.size(); i++) {
                LocalTime[] times = selectedTimes.get(i);
                schedule.append(String.format("%-15s to %-15s : Start at %s hrs - Stops at %s hrs\n",
                        stationNames[path.get(i)],
                        stationNames[path.get(i + 1)],
                        times[0].format(DateTimeFormatter.ofPattern("HH:mm")),
                        times[1].format(DateTimeFormatter.ofPattern("HH:mm"))));

                int journeyMinutes = (int) times[0].until(times[1], java.time.temporal.ChronoUnit.MINUTES);
                totalMinutes += journeyMinutes;

                if (previousArrival != null) {
                    int transferTime = (int) previousArrival.until(times[0], java.time.temporal.ChronoUnit.MINUTES);
                    String warningMessage = transferTime < MIN_TRANSFER_TIME ? 
                        " ⚠ WARNING: This is a tight connection!" : "";
                    schedule.append(String.format("\nYou have a waiting time at Station %s for %d minutes.%s\n\n",
                            stationNames[path.get(i)], transferTime, warningMessage));
                    totalMinutes += transferTime;
                    if (transferTime < MIN_TRANSFER_TIME) {
                        hasTightConnection = true;
                    }
                }

                previousArrival = times[1];
            }

            if (!selectedTimes.isEmpty()) {
                schedule.append("\nTotal time = " + totalMinutes + " minutes\n");
                schedule.append("(Including waiting times at transfer stations)\n");
                if (hasTightConnection) {
                    schedule.append("\n⚠ WARNING: This journey includes tight connections!\n");
                    schedule.append("Please be prepared to move quickly between trains.\n");
                }
            }
        }

        resultArea.setText(schedule.toString());
    }
    
    private void dijkstra(int startStation, int[] distances, int[] previousStations) {
        boolean[] visited = new boolean[NUM_STATIONS];
        Arrays.fill(distances, INFINITY);
        Arrays.fill(previousStations, -1);
        distances[startStation] = 0;
        
        for (int i = 0; i < NUM_STATIONS; i++) {
            int minStation = -1;
            int minDistance = INFINITY;
            
            for (int j = 0; j < NUM_STATIONS; j++) {
                if (!visited[j] && distances[j] < minDistance) {
                    minStation = j;
                    minDistance = distances[j];
                }
            }
            
            if (minStation == -1) break;
            
            visited[minStation] = true;
            
            for (int j = 0; j < NUM_STATIONS; j++) {
                if (!visited[j] && graph[minStation][j] != INFINITY) {
                    int newDist = distances[minStation] + graph[minStation][j];
                    if (newDist < distances[j]) {
                        distances[j] = newDist;
                        previousStations[j] = minStation;
                    }
                }
            }
        }
    }
    
    private ArrayList<Integer> reconstructPath(int[] previousStations, int startStation, int endStation) {
        ArrayList<Integer> path = new ArrayList<>();
        for (int at = endStation; at != -1; at = previousStations[at]) {
            path.add(at);
        }
        Collections.reverse(path);
        
        if (path.get(0) != startStation) {
            return new ArrayList<>();
        }
        
        return path;
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new TicketBookingSystem().setVisible(true);
        });
    }
}