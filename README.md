# ArtSense (BLE)

ArtSense is an Android application designed to empower art enthusiasts and gallery visitors with a deeper understanding and appreciation of art pieces. The application is a part of the [ArtSense Project](https://github.com/xiguaakako/ES-TP). 

## Project Description

ArtSense is built upon Bluetooth Low Energy (BLE) technology to deliver a seamless and informative art viewing experience. Here's how it works:

**For Visitors:**

1. **Proximity Detection:** As visitors navigate the exhibition space, their Android devices with ArtSense installed continuously scan for nearby BLE beacons. These beacons are strategically positioned in close proximity to individual artworks.
2. **Beacon Recognition:** When a visitor's device detects a beacon signal, ArtSense identifies the associated artwork based on the beacon's unique identifier.
3. **Information Display:** The app then dynamically retrieves and displays detailed information about the artwork on the visitor's screen. This information includes:
    - **Artist:** The name of the artist who created the piece.
    - **Title:** The official title of the artwork.
    - **Year of Creation:** The year in which the artwork was completed.
    - **Description:** A comprehensive explanation or analysis of the artwork, providing context and insights.
4. **Self-Guided Tours:** With ArtSense, visitors can embark on self-guided tours, effortlessly accessing relevant information about each artwork as they encounter it. This immersive experience enhances their understanding and appreciation of the art on display.

**For Curators:**

ArtSense empowers curators with robust tools for efficient artwork information management and updates. This is facilitated through a dedicated curator application.

**Content Management:**

  - **Artwork Database:** The curator application maintains a centralized database of artwork information, including titles, artists, descriptions, creation years, and associated images.
  - **Intuitive Interface:** Curators can easily add new artworks, edit existing entries, and delete records through a user-friendly interface within the application.
  - **Real-time Updates:** Any modifications made to the artwork information in the curator application are automatically synchronized with the ArtSense visitor application in real-time. This ensures that visitors always have access to the most up-to-date details about the artworks on display.

**Beacon Management:**

  - **BLE Beacon Association:**  The curator application utilizes Bluetooth Low Energy (BLE) technology to establish and manage the connections between physical BLE beacons and individual artworks.
  - **STM32 Microcontroller Integration:** An STM32 microcontroller board acts as a central hub for controlling and configuring the BLE beacons within the exhibition space.
  - **Remote Beacon Control:** Curators can remotely assign specific beacons to artworks through the application interface. This involves selecting an artwork from the database and pairing it with a corresponding beacon identifier.
  - **Dynamic Beacon Mapping:** The application provides a visual representation of the exhibition layout, allowing curators to easily view and modify the beacon-artwork associations.
  - **Streamlined Exhibition Updates:** The ability to remotely manage beacon assignments significantly simplifies the process of updating exhibitions. Curators can quickly reconfigure the beacons to reflect changes in artwork placement or exhibition content without physically interacting with the beacons.
  - **Enhanced Flexibility:** The integration of BLE technology and the STM32 microcontroller provides curators with greater flexibility in designing and managing interactive art experiences for visitors.

## Project Setup and Launch

1. Clone the Project:
   * Open Android Studio.
   * Click on **Get from VCS**.
   * Choose **Git** and enter the repository URL of the project.
   * Specify the directory where you want to clone the project.
   * Click **Clone**.
2. Open the Project:
   * Once the cloning process is complete, Android Studio will prompt you to open the project. Click **Open**.
3. Build the Project:
   * Android Studio might automatically build the project after it's opened. If not, you can manually trigger a build by clicking **Build** > **Make Project**.
4. Launch the App:
   * Connect a physical Android device to your computer via USB or create a virtual device using the Android Virtual Device (AVD) Manager.
   * Click **Run** > **Run** '**app**' (or press **Shift+F10**).
   * Select your target device from the list and click **OK**. **Troubleshooting**