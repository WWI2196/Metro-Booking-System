# Metro Ticket Booking System

A Java Swing-based desktop application for booking metro tickets with an interactive GUI, real-time scheduling, and dynamic fare calculation.

## Features

- **Interactive Route Planning**
  - Select start and end stations from available metro stations
  - View optimal routes with transfer points
  - Real-time display of available trains

- **Smart Scheduling**
  - Real-time train schedule lookup
  - Multiple train options for each route segment
  - Minimum transfer time validation
  - Operating hours: 06:00 - 20:00
  - Train interval: Every 10 minutes

- **Dynamic Fare Calculation**
  - Distance-based base fare
  - Special discounts for different passenger categories:
    - Students: 50% off
    - Senior Citizens: 40% off
    - Children: 70% off
  - Peak hour pricing (1.5x multiplier)
  - Round trip discount (10% off return journey)

- **Passenger Management**
  - Support for multiple passenger types
  - Up to 10 passengers per booking
  - Mixed passenger categories in single booking

- **Detailed Ticketing**
  - Comprehensive journey details
  - Transfer information with waiting times
  - Fare breakdown
  - Important notices and warnings
  - Connection time alerts

## Technical Details

- Built with Java Swing
- Uses Dijkstra's algorithm for optimal route calculation
- Real-time scheduling system
- Interactive GUI with modern look and feel

## System Requirements

- Java Runtime Environment (JRE) 8 or higher
- Minimum display resolution: 800x600
- Operating System: Windows/Mac/Linux

## Installation

1. Clone the repository:
```bash
git clone https://github.com/WWI2196/Metro-Booking-System.git
```

2. Navigate to the project directory:
```bash
cd metro-ticket-booking
```

3. Compile the Java files:
```bash
javac TicketBookingSystem.java
```

4. Run the application:
```bash
java TicketBookingSystem
```

## Usage Guide

1. **Start the Application**
   - Launch the application to open the main booking interface

2. **Select Journey Details**
   - Choose start and end stations
   - Set desired departure time
   - Select number and types of passengers
   - Optional: Check round trip option

3. **View Available Trains**
   - Click "Find Available Trains"
   - Select preferred trains for each segment
   - Review connection times and warnings

4. **Complete Booking**
   - Review journey summary
   - Check fare breakdown
   - Click "Confirm Booking"
   - Print or save ticket

## Network Map

The system currently supports 6 stations (A through F) with the following connections:

- A ↔ B: 10 km
- A ↔ C: 22 km
- A ↔ E: 8 km
- B ↔ C: 15 km
- B ↔ D: 9 km
- B ↔ F: 7 km
- C ↔ D: 9 km
- D ↔ E: 5 km
- D ↔ F: 12 km
- E ↔ F: 16 km

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request. For major changes, please open an issue first to discuss what you would like to change.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Notes

- The system includes a test mode for simulating different times of day
- All fares are calculated in the local currency
- The system automatically validates connection times and warns about tight connections
- Peak hours are 08:00-10:00 and 17:00-19:00

## Support

For support, please open an issue in the GitHub repository or contact the development team.
