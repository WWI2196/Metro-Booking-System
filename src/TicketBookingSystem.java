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
    private JPanel trainSelectionPanel;
    private ArrayList<LocalTime[]> selectedTimes;
    
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
        JPanel inputPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        startStationCombo = new JComboBox<>(stationNames);
        endStationCombo = new JComboBox<>(stationNames);
        
        SpinnerDateModel timeModel = new SpinnerDateModel();
        timeSpinner = new JSpinner(timeModel);
        JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(timeSpinner, "HH:mm");
        timeSpinner.setEditor(timeEditor);
        
        inputPanel.add(new JLabel("Start Station:"));
        inputPanel.add(startStationCombo);
        inputPanel.add(new JLabel("End Station:"));
        inputPanel.add(endStationCombo);
        inputPanel.add(new JLabel("Desired Departure Time:"));
        inputPanel.add(timeSpinner);
        
        findPathButton = new JButton("Find Available Trains");
        findPathButton.addActionListener(e -> findPath());
        
        inputPanel.add(new JLabel(""));
        inputPanel.add(findPathButton);
        
        // Train Selection Panel
        trainSelectionPanel = new JPanel();
        trainSelectionPanel.setLayout(new BoxLayout(trainSelectionPanel, BoxLayout.Y_AXIS));
        trainSelectionPanel.setBorder(BorderFactory.createTitledBorder("Available Trains"));
        
        // Result Area
        resultArea = new JTextArea(15, 40);
        resultArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(resultArea);
        
        // Main Panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(trainSelectionPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Add components to frame
        add(inputPanel, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);
        
        pack();
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(800, 600));
    }
    
    private void findPath() {
        trainSelectionPanel.removeAll();
        selectedTimes.clear();
        
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
        ArrayList<Integer> path = reconstructPath(previousStations, startStation, endStation);
        
        if (path.isEmpty()) {
            resultArea.setText("No route available between selected stations.");
            return;
        }
        
        // Generate available trains
        generateTrainOptions(path, selectedTime);
        
        trainSelectionPanel.revalidate();
        trainSelectionPanel.repaint();
    }
    
    private void generateTrainOptions(ArrayList<Integer> path, LocalTime desiredTime) {
        trainSelectionPanel.removeAll();
        selectedTimes.clear();
        
        // For each segment of the journey
        for (int i = 0; i < path.size() - 1; i++) {
            int from = path.get(i);
            int to = path.get(i + 1);
            int distance = graph[from][to];
            
            // Calculate travel time for this segment
            int travelMinutes = (int) Math.ceil((distance / (double) TRAIN_SPEED) * 60);
            
            // Get available trains for this segment
            LocalTime baseTime = (i == 0) ? desiredTime : 
                               selectedTimes.isEmpty() ? desiredTime :
                               selectedTimes.get(i-1)[1].plusMinutes(MIN_TRANSFER_TIME);
                               
            ArrayList<LocalTime[]> availableTrains = getAvailableTrains(baseTime, travelMinutes);
            
            // Create selection panel for this segment
            JPanel segmentPanel = new JPanel();
            segmentPanel.setLayout(new BoxLayout(segmentPanel, BoxLayout.Y_AXIS));
            segmentPanel.setBorder(BorderFactory.createTitledBorder(
                String.format("Select train from %s to %s", stationNames[from], stationNames[to])));
            
            ButtonGroup group = new ButtonGroup();
            final int segmentIndex = i;
            
            for (LocalTime[] trainTimes : availableTrains) {
                JRadioButton trainOption = new JRadioButton(String.format(
                    "Depart: %s - Arrive: %s", 
                    trainTimes[0].format(DateTimeFormatter.ofPattern("HH:mm")),
                    trainTimes[1].format(DateTimeFormatter.ofPattern("HH:mm"))));
                    
                trainOption.addActionListener(e -> {
                    while (selectedTimes.size() > segmentIndex) {
                        selectedTimes.remove(selectedTimes.size() - 1);
                    }
                    selectedTimes.add(trainTimes);
                    updateSchedule(path);
                });
                
                group.add(trainOption);
                segmentPanel.add(trainOption);
            }
            
            trainSelectionPanel.add(segmentPanel);
        }
    }
    
    private ArrayList<LocalTime[]> getAvailableTrains(LocalTime baseTime, int travelMinutes) {
        ArrayList<LocalTime[]> trains = new ArrayList<>();
        LocalTime startWindow = baseTime.minusMinutes(10);
        LocalTime endWindow = baseTime.plusMinutes(70); // 1 hour plus 10 minutes
        
        LocalTime currentTrain = startWindow.withMinute((startWindow.getMinute() / TRAIN_INTERVAL) * TRAIN_INTERVAL);
        
        while (!currentTrain.isAfter(endWindow)) {
            if (!currentTrain.isBefore(FIRST_TRAIN) && !currentTrain.isAfter(LAST_TRAIN)) {
                LocalTime arrival = currentTrain.plusMinutes(travelMinutes);
                if (!arrival.isAfter(LAST_TRAIN)) {
                    trains.add(new LocalTime[]{currentTrain, arrival});
                }
            }
            currentTrain = currentTrain.plusMinutes(TRAIN_INTERVAL);
        }
        
        return trains;
    }
    
    private void updateSchedule(ArrayList<Integer> path) {
    if (selectedTimes.size() != path.size() - 1) {
        return;
    }

    StringBuilder schedule = new StringBuilder();
    schedule.append("Trip ").append(stationNames[path.get(0)])
            .append(" to ").append(stationNames[path.get(path.size() - 1)])
            .append("\n--------------\n");

    int totalMinutes = 0;
    LocalTime previousArrival = null;

    for (int i = 0; i < path.size() - 1; i++) {
        LocalTime[] times = selectedTimes.get(i);
        schedule.append(String.format("%s to %s : Start at %s - Stops at %s\n",
                stationNames[path.get(i)],
                stationNames[path.get(i + 1)],
                times[0].format(DateTimeFormatter.ofPattern("HH:mm")),
                times[1].format(DateTimeFormatter.ofPattern("HH:mm"))));

        // Add journey time
        totalMinutes += times[0].until(times[1], java.time.temporal.ChronoUnit.MINUTES);

        // Add transfer time if this isn't the last segment
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