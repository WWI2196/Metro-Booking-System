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
        inputPanel.add(new JLabel("Departure Time:"));
        inputPanel.add(timeSpinner);
        
        findPathButton = new JButton("Find Routes");
        findPathButton.addActionListener(e -> findPath());
        
        inputPanel.add(new JLabel(""));
        inputPanel.add(findPathButton);
        
        // Result Area
        resultArea = new JTextArea(15, 40);
        resultArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(resultArea);
        
        // Add components to frame
        add(inputPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        
        pack();
        setLocationRelativeTo(null);
    }
    
    private void findPath() {
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
        
        // Generate schedule
        generateSchedule(path, selectedTime);
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
    
    private void generateSchedule(ArrayList<Integer> path, LocalTime startTime) {
        StringBuilder schedule = new StringBuilder();
        schedule.append("Trip ").append(stationNames[path.get(0)])
                .append(" to ").append(stationNames[path.get(path.size() - 1)])
                .append("\n--------------\n");
        
        LocalTime currentTime = startTime;
        int totalMinutes = 0;
        
        for (int i = 0; i < path.size() - 1; i++) {
            int from = path.get(i);
            int to = path.get(i + 1);
            int distance = graph[from][to];
            
            // Calculate travel time in minutes
            int travelMinutes = (int) Math.ceil((distance / (double) TRAIN_SPEED) * 60);
            
            LocalTime arrivalTime = currentTime.plusMinutes(travelMinutes);
            
            schedule.append(String.format("%s to %s : Start at %s - Stops at %s\n",
                    stationNames[from],
                    stationNames[to],
                    currentTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                    arrivalTime.format(DateTimeFormatter.ofPattern("HH:mm"))));
            
            totalMinutes += travelMinutes;
            
            // Add waiting time for next leg
            if (i < path.size() - 2) {
                currentTime = arrivalTime.plusMinutes(STATION_WAIT_TIME);
                totalMinutes += STATION_WAIT_TIME;
            }
        }
        
        schedule.append(String.format("\nTotal time = %d minutes", totalMinutes));
        resultArea.setText(schedule.toString());
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