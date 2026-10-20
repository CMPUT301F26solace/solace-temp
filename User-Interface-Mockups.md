# User Interface Mockups & Screen Descriptions

**Project Requirement Coverage:** `US 01.01.01` through `US 03.07.01`

---

## 1. Entrant User Interface Flow

| 1. QR Code Scan | 2. Profile & Settings | 3. Event Details | 4. Lottery Notifications |
| :---: | :---: | :---: | :---: |
| <img width="412" alt="Entrance QR scan" src="https://github.com/user-attachments/assets/3d1cd6be-1348-43ee-99e9-3ff736c54b44" /> | <img width="412" alt="Entrant profile settings" src="https://github.com/user-attachments/assets/092a602b-50fd-4a56-9459-9c30e15e7023" /> | <img width="412" alt="Event details waiting list" src="https://github.com/user-attachments/assets/6a49fa79-839c-432c-b0a7-4be867a07bab" /> | <img width="412" alt="Notifications lottery status" src="https://github.com/user-attachments/assets/e65addca-5a41-4060-a264-dbd8d7adf669" /> |

### Entrant Screen Descriptions

#### 1. Entrance & QR Code Scan Screen `[US 01.06.01, US 01.06.02]`
- **Purpose:** Allows prospective event entrants to scan promotional QR codes using their device camera or search manually.
- **Key Components:** Camera overlay box with viewfinder alignment markers, primary "Scan QR Code" button, manual input search bar, and fixed bottom navigation bar.

#### 2. Entrant Profile & Settings Screen `[US 01.02.01, US 01.02.02, US 01.03.01, US 01.03.02, US 01.03.03, US 01.04.03, US 01.07.01]`
- **Purpose:** Manages entrant profile details, preferences, and identification without requiring passwords.
- **Key Components:** Deterministic default avatar generated from initials, custom profile image upload/remove actions, form fields for Name, Email, and Phone Number, notification opt-out toggle switch, and read-only Device ID indicator tag.

#### 3. Event Details & Waiting List Screen `[US 01.01.01, US 01.01.02, US 01.08.01]`
- **Purpose:** Displays complete event information and enables joining/leaving the event waiting list.
- **Key Components:** High-resolution event poster header banner, event title, price ($60), registration period, geolocation requirement warning badge, primary "Join Waiting List" button, and "Leave Waiting List" button.

#### 4. Entrant Notifications & Lottery Status Screen `[US 01.04.01, US 01.04.02, US 01.05.01, US 01.05.02, US 01.05.03]`
- **Purpose:** Displays real-time updates and lottery draw results for joined events.
- **Key Components:** Selected "Won Lottery" card with prominent "Accept Invitation" (green) and "Decline Invitation" (red) action buttons, and "Not Selected - Waiting List" status cards.

---

## 2. Organizer User Interface Flow

| 5. Create Event | 6. Facility Profile | 7. Waiting List & Map | 8. Lottery Sampling |
| :---: | :---: | :---: | :---: |
| <img width="412" alt="Organizer Create Event" src="https://github.com/user-attachments/assets/3e1e4b6a-ba65-4766-848b-ce2c2e35635b" /> | <img width="412" alt="Facility Profile" src="https://github.com/user-attachments/assets/f8bec71b-8304-43fe-a570-aad6d3b005b3" /> | <img width="412" alt="Waiting List Entrant Map" src="https://github.com/user-attachments/assets/85edaa6b-306c-4d32-b186-075529b449ba" /> | <img width="412" alt="Lottery Sampling Status Lists" src="https://github.com/user-attachments/assets/abd397db-478e-452b-93b5-d47c9c0d9ba4" /> |

### Organizer Screen Descriptions

#### 5. Organizer Create Event Screen `[US 02.01.01, US 02.01.02, US 02.03.01, US 02.02.03, US 02.04.01, US 02.04.02]`
- **Purpose:** Enables event organizers to configure new events, upload promotional material, and set lottery parameters.
- **Key Components:** Inputs for Title, Dates, Price, and Registration Window; toggle switch for maximum waiting list cap; toggle switch for mandatory geolocation verification; poster upload region; and "Generate QR Code & Publish" button.

#### 6. Facility Profile Management Screen `[US 02.01.03]`
- **Purpose:** Manages facility organization information and contact details.
- **Key Components:** Facility Name, Physical Address, Contact Phone, Contact Email, and "Save Facility Profile" action button.
