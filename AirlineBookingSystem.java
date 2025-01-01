import java.sql.*;
import java.time.LocalDate;
import java.util.Scanner;

public class AirlineBookingSystem {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/AirlineBookingSystem";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "MySQLP@55W0rd52";

    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Scanner scanner = new Scanner(System.in)) {

            int passengerId = -1;

            while (true) {
                System.out.println("\n=== Airline Booking System Menu ===");
                System.out.println("1. Register");
                System.out.println("2. Login");
                System.out.println("3. Book a Flight");
                System.out.println("4. View Bookings for a Flight");
                System.out.println("5. Exit");
                System.out.print("Enter your choice: ");

                int choice = scanner.nextInt();
                scanner.nextLine(); // Consume newline

                switch (choice) {
                    case 1:
                        passengerId = registerPassenger(conn, scanner);
                        break;
                    case 2:
                        passengerId = loginPassenger(conn, scanner);
                        break;
                    case 3:
                        if (passengerId != -1) {
                            bookFlight(conn, scanner, passengerId);
                        } else {
                            System.out.println("Please register or log in first.");
                        }
                        break;
                    case 4:
                        viewBookingsForFlight(conn, scanner);
                        break;
                    case 5:
                        System.out.println("Exiting... Thank you for using the system.");
                        return;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
    }

    private static int registerPassenger(Connection conn, Scanner scanner) {
        try {
            System.out.println("Enter First Name:");
            String firstName = scanner.nextLine();
            System.out.println("Enter Last Name:");
            String lastName = scanner.nextLine();
            System.out.println("Enter Email:");
            String email = scanner.nextLine();
            System.out.println("Enter Phone Number:");
            String phone = scanner.nextLine();

            String sql = "INSERT INTO Passengers (FirstName, LastName, Email, Phone) VALUES (?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, firstName);
                stmt.setString(2, lastName);
                stmt.setString(3, email);
                stmt.setString(4, phone);
                stmt.executeUpdate();

                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    int passengerId = rs.getInt(1);
                    System.out.println("Registration successful. Your Passenger ID: " + passengerId);
                    return passengerId;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error during registration: " + e.getMessage());
        }
        return -1;
    }

    private static int loginPassenger(Connection conn, Scanner scanner) {
        try {
            System.out.println("Enter Email:");
            String email = scanner.nextLine();

            String sql = "SELECT PassengerID FROM Passengers WHERE Email = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, email);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    int passengerId = rs.getInt("PassengerID");
                    System.out.println("Login successful. Your Passenger ID: " + passengerId);
                    return passengerId;
                } else {
                    System.out.println("Email not found. Please register first.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error during login: " + e.getMessage());
        }
        return -1;
    }

    private static void bookFlight(Connection conn, Scanner scanner, int passengerId) {
        try {
            LocalDate today = LocalDate.now();
            LocalDate maxBookingDate = today.plusDays(30);

            System.out.println("Available flights (next 30 days):");
            String sql = "SELECT FlightID, AirlineID, DepartureTime, ArrivalTime, Price FROM Flights " +
                         "WHERE DepartureTime BETWEEN ? AND ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setDate(1, Date.valueOf(today));
                stmt.setDate(2, Date.valueOf(maxBookingDate));
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    int flightId = rs.getInt("FlightID");
                    int airlineId = rs.getInt("AirlineID");
                    Timestamp departureTime = rs.getTimestamp("DepartureTime");
                    Timestamp arrivalTime = rs.getTimestamp("ArrivalTime");
                    double price = rs.getDouble("Price");

                    System.out.printf("FlightID: %d, AirlineID: %d, Departure: %s, Arrival: %s, Price: $%.2f\n",
                            flightId, airlineId, departureTime, arrivalTime, price);
                }
            }

            System.out.println("Enter Flight ID to book:");
            int flightId = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            System.out.println("Enter Seat Number:");
            String seatNumber = scanner.nextLine();

            String insertBookingSql = "INSERT INTO Bookings (PassengerID, FlightID, BookingDate, SeatNumber) VALUES (?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertBookingSql)) {
                stmt.setInt(1, passengerId);
                stmt.setInt(2, flightId);
                stmt.setTimestamp(3, Timestamp.valueOf(java.time.LocalDateTime.now()));
                stmt.setString(4, seatNumber);

                stmt.executeUpdate();
                System.out.println("Flight booked successfully! Seat Number: " + seatNumber);
            }
        } catch (SQLException e) {
            System.err.println("Error during booking: " + e.getMessage());
        }
    }

    private static void viewBookingsForFlight(Connection conn, Scanner scanner) {
        try {
            System.out.println("Enter Flight ID to view bookings:");
            int flightId = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            String sql = "SELECT B.BookingID, P.FirstName, P.LastName, B.SeatNumber, B.BookingDate " +
                         "FROM Bookings B JOIN Passengers P ON B.PassengerID = P.PassengerID " +
                         "WHERE B.FlightID = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, flightId);
                ResultSet rs = stmt.executeQuery();

                System.out.println("\nBookings for Flight ID: " + flightId);
                while (rs.next()) {
                    int bookingId = rs.getInt("BookingID");
                    String firstName = rs.getString("FirstName");
                    String lastName = rs.getString("LastName");
                    String seatNumber = rs.getString("SeatNumber");
                    Timestamp bookingDate = rs.getTimestamp("BookingDate");

                    System.out.printf("BookingID: %d, Passenger: %s %s, Seat: %s, Date: %s\n",
                            bookingId, firstName, lastName, seatNumber, bookingDate);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error viewing bookings: " + e.getMessage());
        }
    }
}