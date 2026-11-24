import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;
import java.util.Scanner;

public class DoctorAppointmentScheduler {

    private static final String DATA_FILE = "appointments.txt";
    private static final int SLOT_DURATION = 30; // 30 minutes per appointment
    private static final int DAY_START_MINUTES = 9 * 60; // 09:00 AM
    private static final int DAY_END_MINUTES = 18 * 60;  // 06:00 PM

    static class Appointment {
        private final String appointmentId;
        private final String patientName;
        private final int startMinutes;
        private final int durationMinutes;

        public Appointment(String appointmentId, String patientName, int startMinutes, int durationMinutes) {
            this.appointmentId = appointmentId.toUpperCase(Locale.ROOT);
            this.patientName = patientName;
            this.startMinutes = startMinutes;
            this.durationMinutes = durationMinutes;
        }

        public String getAppointmentId() {
            return appointmentId;
        }

        public String getPatientName() {
            return patientName;
        }

        public int getStartMinutes() {
            return startMinutes;
        }

        public int getEndMinutes() {
            return startMinutes + durationMinutes;
        }

        public int getDurationMinutes() {
            return durationMinutes;
        }

        public String toFileString() {
            return appointmentId + "|" + patientName + "|" + formatTime(startMinutes) + "|" + durationMinutes;
        }

        @Override
        public String toString() {
            return appointmentId + " | " + patientName + " | " + formatTime(startMinutes);
        }
    }

    static class BSTNode {
        Appointment appointment;
        BSTNode left;
        BSTNode right;

        BSTNode(Appointment appointment) {
            this.appointment = appointment;
        }
    }

    // Binary Search Tree keyed by appointment start time (minutes from midnight)
    static class AppointmentBST {
        private BSTNode root;

        public void clear() {
            root = null;
        }

        public boolean hasOverlap(int startMin, int durationMin) {
            int endMin = startMin + durationMin;
            return checkOverlapRec(root, startMin, endMin);
        }

        private boolean checkOverlapRec(BSTNode node, int startMin, int endMin) {
            if (node == null) {
                return false;
            }
            int apptStart = node.appointment.getStartMinutes();
            int apptEnd = node.appointment.getEndMinutes();

            // Two intervals [startMin, endMin) and [apptStart, apptEnd) overlap iff startMin < apptEnd && apptStart < endMin
            if (startMin < apptEnd && apptStart < endMin) {
                return true;
            }
            if (startMin < apptStart) {
                return checkOverlapRec(node.left, startMin, endMin);
            } else {
                return checkOverlapRec(node.right, startMin, endMin);
            }
        }

        public boolean insert(Appointment appt) {
            if (hasOverlap(appt.getStartMinutes(), appt.getDurationMinutes())) {
                return false;
            }
            root = insertRec(root, appt);
            return true;
        }

        private BSTNode insertRec(BSTNode node, Appointment appt) {
            if (node == null) {
                return new BSTNode(appt);
            }
            if (appt.getStartMinutes() < node.appointment.getStartMinutes()) {
                node.left = insertRec(node.left, appt);
            } else {
                node.right = insertRec(node.right, appt);
            }
            return node;
        }

        public Appointment searchById(String id) {
            return searchByIdRec(root, id.trim().toUpperCase(Locale.ROOT));
        }

        private Appointment searchByIdRec(BSTNode node, String targetId) {
            if (node == null) {
                return null;
            }
            if (node.appointment.getAppointmentId().equals(targetId)) {
                return node.appointment;
            }
            Appointment leftRes = searchByIdRec(node.left, targetId);
            if (leftRes != null) {
                return leftRes;
            }
            return searchByIdRec(node.right, targetId);
        }

        public boolean cancelById(String id) {
            String targetId = id.trim().toUpperCase(Locale.ROOT);
            if (searchById(targetId) == null) {
                return false;
            }
            root = deleteByIdRec(root, targetId);
            return true;
        }

        private BSTNode deleteByIdRec(BSTNode node, String targetId) {
            if (node == null) {
                return null;
            }
            if (node.appointment.getAppointmentId().equals(targetId)) {
                if (node.left == null) {
                    return node.right;
                }
                if (node.right == null) {
                    return node.left;
                }
                BSTNode minNode = findMin(node.right);
                node.appointment = minNode.appointment;
                node.right = deleteByIdRec(node.right, minNode.appointment.getAppointmentId());
                return node;
            }
            node.left = deleteByIdRec(node.left, targetId);
            node.right = deleteByIdRec(node.right, targetId);
            return node;
        }

        private BSTNode findMin(BSTNode node) {
            while (node.left != null) {
                node = node.left;
            }
            return node;
        }

        // Greedy earliest-slot strategy: picks the earliest non-overlapping 30-min slot from 'fromMinutes'
        public int findEarliestSlot(int fromMinutes) {
            int candidate = Math.max(fromMinutes, DAY_START_MINUTES);
            while (candidate + SLOT_DURATION <= DAY_END_MINUTES) {
                if (!hasOverlap(candidate, SLOT_DURATION)) {
                    return candidate;
                }
                candidate += SLOT_DURATION;
            }
            return -1;
        }

        public void displayInOrder() {
            if (root == null) {
                System.out.println("No scheduled appointments.");
                return;
            }
            inOrderRec(root);
        }

        private void inOrderRec(BSTNode node) {
            if (node == null) {
                return;
            }
            inOrderRec(node.left);
            System.out.println("  - " + node.appointment);
            inOrderRec(node.right);
        }

        public void writeToFile(BufferedWriter writer) throws IOException {
            writeInOrderRec(root, writer);
        }

        private void writeInOrderRec(BSTNode node, BufferedWriter writer) throws IOException {
            if (node == null) {
                return;
            }
            writeInOrderRec(node.left, writer);
            writer.write(node.appointment.toFileString());
            writer.newLine();
            writeInOrderRec(node.right, writer);
        }
    }

    static class PendingRequestQueue {
        private final Queue<String> requests = new LinkedList<>();

        public void enqueue(String requestSummary) {
            requests.offer(requestSummary);
        }

        public void displayPending() {
            if (requests.isEmpty()) {
                System.out.println("Pending request queue is empty.");
                return;
            }
            for (String req : requests) {
                System.out.println("  - " + req);
            }
        }
    }

    private final AppointmentBST bst = new AppointmentBST();
    private final PendingRequestQueue pendingQueue = new PendingRequestQueue();
    private int nextIdNumber = 101;

    public DoctorAppointmentScheduler() {
        loadFromFile();
    }

    // Parses times like "10:00 AM", "09:00 AM", "10:30 AM", "2:00 PM", or "10:00" (24h)
    public static int parseTimeToMinutes(String rawTime) {
        if (rawTime == null) {
            return -1;
        }
        String s = rawTime.trim().toUpperCase(Locale.ROOT);
        boolean hasAm = s.endsWith("AM");
        boolean hasPm = s.endsWith("PM");
        if (hasAm || hasPm) {
            s = s.substring(0, s.length() - 2).trim();
        }
        String[] parts = s.split(":");
        if (parts.length != 2) {
            return -1;
        }
        try {
            int hour = Integer.parseInt(parts[0].trim());
            int minute = Integer.parseInt(parts[1].trim());
            if (minute < 0 || minute >= 60) {
                return -1;
            }
            if (hasAm || hasPm) {
                if (hour < 1 || hour > 12) {
                    return -1;
                }
                if (hasAm) {
                    hour = (hour == 12) ? 0 : hour;
                } else {
                    hour = (hour == 12) ? 12 : hour + 12;
                }
            } else {
                if (hour < 0 || hour > 23) {
                    return -1;
                }
            }
            return hour * 60 + minute;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static String formatTime(int totalMinutes) {
        int h24 = (totalMinutes / 60) % 24;
        int min = totalMinutes % 60;
        String period = (h24 >= 12) ? "PM" : "AM";
        int h12 = h24 % 12;
        if (h12 == 0) {
            h12 = 12;
        }
        return String.format(Locale.ROOT, "%02d:%02d %s", h12, min, period);
    }

    public String bookAppointment(String patientName, String rawTime) {
        String name = (patientName == null) ? "" : patientName.trim();
        if (name.isEmpty()) {
            return "Invalid input: Patient name cannot be empty.";
        }
        int startMin = parseTimeToMinutes(rawTime);
        if (startMin < 0 || startMin + SLOT_DURATION > 24 * 60) {
            return "Invalid appointment time: " + rawTime;
        }
        if (bst.hasOverlap(startMin, SLOT_DURATION)) {
            pendingQueue.enqueue(name + " requested " + formatTime(startMin));
            return "Overlapping appointment rejected for " + name + " at " + formatTime(startMin) + " (added to pending queue).";
        }
        String apptId = "A" + (nextIdNumber++);
        Appointment appt = new Appointment(apptId, name, startMin, SLOT_DURATION);
        bst.insert(appt);
        return "Appointment booked: " + appt;
    }

    public String searchAppointment(String apptId) {
        String id = (apptId == null) ? "" : apptId.trim().toUpperCase(Locale.ROOT);
        Appointment found = bst.searchById(id);
        if (found == null) {
            return "Appointment " + id + " not found.";
        }
        return "Appointment found: " + found + " (" + found.getDurationMinutes() + " mins)";
    }

    public String cancelAppointment(String apptId) {
        String id = (apptId == null) ? "" : apptId.trim().toUpperCase(Locale.ROOT);
        if (bst.cancelById(id)) {
            return "Appointment " + id + " cancelled.";
        }
        return "Appointment " + id + " not found.";
    }

    public String earliestAvailableSlot(String fromTimeStr) {
        int startFrom = DAY_START_MINUTES;
        if (fromTimeStr != null && !fromTimeStr.trim().isEmpty()) {
            int parsed = parseTimeToMinutes(fromTimeStr);
            if (parsed < 0) {
                return "Invalid start time: " + fromTimeStr;
            }
            startFrom = parsed;
        }
        int slot = bst.findEarliestSlot(startFrom);
        if (slot < 0) {
            return "No available 30-minute slot found from " + formatTime(startFrom) + ".";
        }
        return "Earliest available slot: " + formatTime(slot);
    }

    public void loadFromFile() {
        File file = new File(DATA_FILE);
        if (!file.exists()) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split("\\|");
                if (parts.length >= 4) {
                    String id = parts[0].trim().toUpperCase(Locale.ROOT);
                    String patient = parts[1].trim();
                    int startMin = parseTimeToMinutes(parts[2].trim());
                    int dur = Integer.parseInt(parts[3].trim());
                    if (startMin >= 0 && dur > 0) {
                        bst.insert(new Appointment(id, patient, startMin, dur));
                        if (id.startsWith("A")) {
                            try {
                                int num = Integer.parseInt(id.substring(1));
                                if (num >= nextIdNumber) {
                                    nextIdNumber = num + 1;
                                }
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                }
            }
            System.out.println("[INFO] Loaded appointments from " + DATA_FILE);
        } catch (IOException | NumberFormatException e) {
            System.out.println("[WARN] Could not load " + DATA_FILE + ": " + e.getMessage());
        }
    }

    public void saveToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(DATA_FILE))) {
            bst.writeToFile(writer);
            System.out.println("[INFO] Appointments saved to " + DATA_FILE);
        } catch (IOException e) {
            System.out.println("[ERROR] Failed to save " + DATA_FILE + ": " + e.getMessage());
        }
    }

    public void runStandardTestCases() {
        System.out.println("\n--- Running Standard Test Cases (PS No. 60) ---");

        // Test Case 1: Normal Input -> Book Riya at 10:00 AM
        bst.clear();
        nextIdNumber = 101;
        System.out.println("Test Case 1 (Normal): " + bookAppointment("Riya", "10:00 AM"));

        // Test Case 2: Boundary / Minimum Input -> No appointments exist; request earliest available slot
        bst.clear();
        System.out.println("Test Case 2 (Boundary/Empty): " + earliestAvailableSlot("09:00 AM"));

        // Test Case 3: Invalid Input -> Search appointment A999
        System.out.println("Test Case 3 (Invalid Search): " + searchAppointment("A999"));

        // Test Case 4: Different Valid Scenario -> Appointments at 10:00 and 11:00 exist; request earliest slot from 10:00
        bst.clear();
        nextIdNumber = 101;
        bookAppointment("Riya", "10:00 AM");
        bookAppointment("Kabir", "11:00 AM");
        int earliestFrom10 = bst.findEarliestSlot(parseTimeToMinutes("10:00 AM"));
        System.out.println("Test Case 4 (Valid Scenario): Earliest slot from 10:00 AM -> Appointment available at " + formatTime(earliestFrom10) + ".");
    }

    public static void main(String[] args) {
        DoctorAppointmentScheduler scheduler = new DoctorAppointmentScheduler();
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n==========================================");
            System.out.println("  DOCTOR APPOINTMENT SCHEDULER (PS No. 60)");
            System.out.println("==========================================");
            System.out.println("1. Book Appointment (30 mins)");
            System.out.println("2. Cancel Appointment by ID");
            System.out.println("3. Search Appointment by ID");
            System.out.println("4. Display Scheduled Appointments");
            System.out.println("5. Find Earliest Available Slot");
            System.out.println("6. Run 4 Standard Test Cases");
            System.out.println("7. Save & Exit");
            System.out.print("Enter your choice: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    System.out.print("Enter Patient Name: ");
                    String name = scanner.nextLine();
                    System.out.print("Enter Time (e.g., 10:00 AM): ");
                    String timeStr = scanner.nextLine();
                    System.out.println(scheduler.bookAppointment(name, timeStr));
                    break;
                case "2":
                    System.out.print("Enter Appointment ID to cancel (e.g., A101): ");
                    String cancelId = scanner.nextLine();
                    System.out.println(scheduler.cancelAppointment(cancelId));
                    break;
                case "3":
                    System.out.print("Enter Appointment ID to search (e.g., A101 or A999): ");
                    String searchId = scanner.nextLine();
                    System.out.println(scheduler.searchAppointment(searchId));
                    break;
                case "4":
                    System.out.println("\n--- Scheduled Appointments (Sorted by Time) ---");
                    scheduler.bst.displayInOrder();
                    System.out.println("\n--- Pending Requests Queue ---");
                    scheduler.pendingQueue.displayPending();
                    break;
                case "5":
                    System.out.print("Enter start time to search from (or press Enter for 09:00 AM): ");
                    String fromTime = scanner.nextLine();
                    System.out.println(scheduler.earliestAvailableSlot(fromTime));
                    break;
                case "6":
                    scheduler.runStandardTestCases();
                    break;
                case "7":
                    scheduler.saveToFile();
                    scanner.close();
                    return;
                default:
                    System.out.println("Invalid choice. Please select 1 to 7.");
            }
        }
    }
}