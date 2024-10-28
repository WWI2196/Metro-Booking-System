import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class TicketBookingSystem extends JFrame {
    private static final int INFINITY = Integer.MAX_VALUE;
    private static final int NUM_STATIONS = 6;
    private static final int TRAIN_SPEED = 30; // km/h
    private static final int STATION_WAIT_TIME = 10; // minutes
    private static final int MIN_TRANSFER_TIME = 5; // minutes
    private static final LocalTime FIRST_TRAIN = LocalTime.of(6, 0);
    private static final LocalTime LAST_TRAIN = LocalTime.of(20, 0);
    private static final int TRAIN_INTERVAL = 20; // minutes between trains
    
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
    
    private void initializeGraph() {
        // Initialize with infinity (no connection)
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
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        startStationCombo = new JComboBox<>(stationNames);
        endStationCombo = new JComboBox<>(stationNames);
        
        SpinnerDateModel timeModel = new SpinnerDateModel();
        timeSpinner = new JSpinner(timeModel);
        JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(timeSpinner, "HH:mm");
        timeSpinner.setEditor(timeEditor);
        
        // Add components with GridBagLayout
        gbc.gridx = 0; gbc.gridy = 0;
        inputPanel.add(new JLabel("Start Station:"), gbc);
        
        gbc.gridx = 1;
        inputPanel.add(startStationCombo, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        inputPanel.add(new JLabel("End Station:"), gbc);
        
        gbc.gridx = 1;
        inputPanel.add(endStationCombo, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        inputPanel.add(new JLabel("Desired Departure Time:"), gbc);
        
        gbc.gridx = 1;
        inputPanel.add(timeSpinner, gbc);
        
        findPathButton = new JButton("Find Available Trains");
        findPathButton.addActionListener(e -> findPath());
        
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 2;
        inputPanel.add(findPathButton, gbc);
        
        confirmButton = new JButton("Confirm Booking");
        confirmButton.setEnabled(false);
        confirmButton.addActionListener(e -> showTicket());
        
        // Train Selection Panel with horizontal layout
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
        
        // Add components to frame
        add(inputPanel, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);
        
        pack();
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(800, 600));
    }
    
    private ArrayList<LocalTime[]> getAvailableTrains(LocalTime currentTime, int travelMinutes) {
        ArrayList<LocalTime[]> trains = new ArrayList<>();
        LocalTime endWindow = currentTime.plusMinutes(60); // Show trains for the next hour only
        
        // Round up to the next available train time
        LocalTime nextTrainTime = currentTime;
        int minutes = nextTrainTime.getMinute();
        int roundedMinutes = ((minutes + TRAIN_INTERVAL - 1) / TRAIN_INTERVAL) * TRAIN_INTERVAL;
        
        // Handle the case where roundedMinutes equals 60
        if (roundedMinutes == 60) {
            nextTrainTime = nextTrainTime.plusHours(1).withMinute(0);
        } else {
            nextTrainTime = nextTrainTime.withMinute(roundedMinutes);
        }
        nextTrainTime = nextTrainTime.withSecond(0).withNano(0);
        
        while (!nextTrainTime.isAfter(endWindow)) {
            if (!nextTrainTime.isBefore(currentTime) && !nextTrainTime.isAfter(LAST_TRAIN)) {
                LocalTime arrival = nextTrainTime.plusMinutes(travelMinutes);
                if (!arrival.isAfter(LAST_TRAIN)) {
                    trains.add(new LocalTime[]{nextTrainTime, arrival});
                }
            }
            nextTrainTime = nextTrainTime.plusMinutes(TRAIN_INTERVAL);
        }
        
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
        
        if (selectedTime.isBefore(FIRST_TRAIN) || selectedTime.isAfter(LAST_TRAIN)) {
            JOptionPane.showMessageDialog(this, 
                "Trains operate only between 06:00 and 20:00.");
            return;
        }
        
        // Find shortest path
        int[] distances = new int[NUM_STATIONS];
        int[] previousStations = new int[NUM_STATIONS];
        dijkstra(startStation, distances, previousStations);
        
        // Generate route
        currentPath = reconstructPath(previousStations, startStation, endStation);
        
        if (currentPath.isEmpty()) {
            resultArea.setText("No route available between selected stations.");
            return;
        }
        
        // Generate available trains
        generateTrainOptions(currentPath, selectedTime);
        
        trainSelectionPanel.revalidate();
        trainSelectionPanel.repaint();
    }
    
    private void generateTrainOptions(ArrayList<Integer> path, LocalTime desiredTime) {
        trainSelectionPanel.removeAll();
        selectedTimes.clear();
        
        // Create a panel for each segment that will be displayed horizontally
        JPanel segmentsPanel = new JPanel();
        segmentsPanel.setLayout(new BoxLayout(segmentsPanel, BoxLayout.X_AXIS));
        
        LocalTime currentTime = desiredTime;
        
        for (int i = 0; i < path.size() - 1; i++) {
            int from = path.get(i);
            int to = path.get(i + 1);
            int distance = graph[from][to];
            
            int travelMinutes = (int) Math.ceil((distance / (double) TRAIN_SPEED) * 60);
            
            // Get available trains for this segment, considering arrival time from previous segment
            ArrayList<LocalTime[]> availableTrains = getAvailableTrains(currentTime, travelMinutes);
            
            if (availableTrains.isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "No available trains found for segment " + stationNames[from] + " to " + stationNames[to]);
                return;
            }
            
            // Create segment panel
            JPanel segmentPanel = new JPanel();
            segmentPanel.setLayout(new BoxLayout(segmentPanel, BoxLayout.Y_AXIS));
            segmentPanel.setBorder(BorderFactory.createTitledBorder(
                String.format("%s to %s", stationNames[from], stationNames[to])));
            
            ButtonGroup group = new ButtonGroup();
            final int segmentIndex = i;
            
            boolean firstTrain = true;
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
                    
                    // Update next segment's available time
                    if (segmentIndex < path.size() - 2) {
                        LocalTime nextSegmentStartTime = trainTimes[1].plusMinutes(MIN_TRANSFER_TIME);
                        regenerateNextSegment(path, segmentIndex + 1, nextSegmentStartTime);
                    }
                });
                
                group.add(trainOption);
                segmentPanel.add(trainOption);
                
                if (firstTrain) {
                    trainOption.setSelected(true);
                    selectedTimes.add(trainTimes);
                    currentTime = trainTimes[1].plusMinutes(MIN_TRANSFER_TIME);
                    firstTrain = false;
                }
            }
            
            // Add some spacing between segments
            segmentPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(5, 5, 5, 5),
                segmentPanel.getBorder()));
            
            segmentsPanel.add(segmentPanel);
            if (i < path.size() - 2) {
                segmentsPanel.add(Box.createRigidArea(new Dimension(10, 0)));
            }
        }
        
        trainSelectionPanel.add(segmentsPanel);
        validateSelection();
    }
    
    private void regenerateNextSegment(ArrayList<Integer> path, int segmentIndex, LocalTime startTime) {
        int from = path.get(segmentIndex);
        int to = path.get(segmentIndex + 1);
        int distance = graph[from][to];
        
        int travelMinutes = (int) Math.ceil((distance / (double) TRAIN_SPEED) * 60);
        ArrayList<LocalTime[]> availableTrains = getAvailableTrains(startTime, travelMinutes);
        
        // Update the UI for the next segment
        Component[] components = trainSelectionPanel.getComponents();
        if (components.length > 0 && components[0] instanceof JPanel) {
            JPanel segmentsPanel = (JPanel) components[0];
            Component[] segmentPanels = segmentsPanel.getComponents();
            
            if (segmentIndex < segmentPanels.length) {
                JPanel nextSegmentPanel = (JPanel) segmentPanels[segmentIndex * 2]; // Account for spacing components
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
                        
                        // Update next segment if exists
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
            boolean validTimes = true;
            LocalTime previousArrival = null;
            
            for (LocalTime[] times : selectedTimes) {
                if (previousArrival != null) {
                    int transferTime = (int) previousArrival.until(times[0], java.time.temporal.ChronoUnit.MINUTES);
                    if (transferTime < MIN_TRANSFER_TIME) {
                        validTimes = false;
                        break;
                    }
                }
                previousArrival = times[1];
            }
            
            confirmButton.setEnabled(validTimes);
        } else {
            confirmButton.setEnabled(false);
        }
    }
    
    private void showTicket() {
        if (selectedTimes.isEmpty() || currentPath.isEmpty()) {
            return;
        }
        
        StringBuilder ticket = new StringBuilder();
        ticket.append("=================================\n");
        ticket.append("         METRO TICKET            \n");
        ticket.append("=================================\n\n");
        
        ticket.append(String.format("From: Station %s\n", stationNames[currentPath.get(0)]));
        ticket.append(String.format("To: Station %s\n", stationNames[currentPath.get(currentPath.size() - 1)]));
        ticket.append(String.format("Date: %s\n\n", java.time.LocalDate.now()));
        
        ticket.append("Journey Details:\n");
        ticket.append("-----------------\n");
        
        int totalMinutes = 0;
        LocalTime previousArrival = null;
        
        for (int i = 0; i < selectedTimes.size(); i++) {
            LocalTime[] times = selectedTimes.get(i);
            int from = currentPath.get(i);
            int to = currentPath.get(i + 1);
            
            if (previousArrival != null) {
                int transferTime = (int) previousArrival.until(times[0], java.time.temporal.ChronoUnit.MINUTES);
                ticket.append(String.format("Transfer time at Station %s: %d minutes\n",
                    stationNames[from], transferTime));
                totalMinutes += transferTime;
            }
            
            int journeyMinutes = (int) times[0].until(times[1], java.time.temporal.ChronoUnit.MINUTES);
            ticket.append(String.format("Train %d: %s to %s\n", i + 1, stationNames[from], stationNames[to]));
            ticket.append(String.format("Departure: %s\n", times[0].format(DateTimeFormatter.ofPattern("HH:mm"))));
            ticket.append(String.format("Arrival: %s\n", times[1].format(DateTimeFormatter.ofPattern("HH:mm"))));
            ticket.append(String.format("Journey time: %d minutes\n\n", journeyMinutes));
            
            totalMinutes += journeyMinutes;
            previousArrival = times[1];
        }
        
        ticket.append("---------------------------------\n");
        ticket.append(String.format("Total journey time: %d minutes\n", totalMinutes));
        ticket.append("Including transfers and waiting times\n");
        ticket.append("\nImportant Notes:\n");
        ticket.append("* Please arrive at least 5 minutes before departure\n");
        ticket.append("* Station waiting time: 10 minutes\n");
        ticket.append("* Minimum transfer time: 5 minutes\n");
        ticket.append("=================================\n");
        
        JTextArea ticketArea = new JTextArea(ticket.toString());
        ticketArea.setEditable(false);
        ticketArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(ticketArea);
        scrollPane.setPreferredSize(new Dimension(400, 500));
        
        JDialog ticketDialog = new JDialog(this, "Your Ticket", true);
        ticketDialog.add(scrollPane);
        ticketDialog.pack();
        ticketDialog.setLocationRelativeTo(this);
        ticketDialog.setVisible(true);
    }
    
    private void updateSchedule(ArrayList<Integer> path) {
        if (selectedTimes.isEmpty()) {
            return;
        }

        StringBuilder schedule = new StringBuilder();
        schedule.append("Trip ").append(stationNames[path.get(0)])
                .append(" to ").append(stationNames[path.get(path.size() - 1)])
                .append("\n--------------\n");

        int totalMinutes = 0;
        LocalTime previousArrival = null;

        for (int i = 0; i < selectedTimes.size(); i++) {
            LocalTime[] times = selectedTimes.get(i);
            schedule.append(String.format("%s to %s : Start at %s - Stops at %s\n",
                    stationNames[path.get(i)],
                    stationNames[path.get(i + 1)],
                    times[0].format(DateTimeFormatter.ofPattern("HH:mm")),
                    times[1].format(DateTimeFormatter.ofPattern("HH:mm"))));

            // Calculate journey time for this segment
            int journeyMinutes = (int) times[0].until(times[1], java.time.temporal.ChronoUnit.MINUTES);
            totalMinutes += journeyMinutes;

            // Add transfer time if there's a previous segment
            if (previousArrival != null) {
                int transferTime = (int) previousArrival.until(times[0], java.time.temporal.ChronoUnit.MINUTES);
                totalMinutes += transferTime;
            }

            previousArrival = times[1];
        }

        schedule.append(String.format("\nTotal time = %d minutes", totalMinutes));
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