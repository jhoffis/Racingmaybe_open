
#  Contribution & Redistribution  

- **Redistribution is not allowed**, but you are **free to modify** the game and contribute!  
- If you submit a **pull request**, it is likely to be included in the next patch! ^^

#  Compilation Instructions  

To compile, you will need:  

- **Maven**  
  - Copy the `src` folder into a blank **Java 18+ Maven project**  
  - Set the `res` folder as the **resource folder**  
  - Set the `src` folder as the **source folder**

---
##  Required Asset Transfers  
**A copy of the game** to copy over the necessary assets.
Copy the following into the **root** of this project:  
- Make a `res/` folder and copy into here the folders `shaders`, `fonts`, and `linux-x86-64` from inside `bin/racingmaybe.jar`  
- The following folders:  
  - `audio`  
  - `images`  
  - `models`  
- You should now have 5 folders in the root folder; `src`, `res`, `audio`, `images`, and `models`. 
- Then, either **"Update Project"** or **"Install Packages"** using the pom.xml
---

- **Exporting the final `.jar` (optional, using Eclipse)**  
  1. Click **"Export"** → **"Runnable JAR file"**  
  2. Set **Launch Configuration** to `"Main - Racingmaybe"`  
  3. Set **Export Location** inside the `bin/` folder  
  4. Select **"Copy required libraries into a sub-folder next to the generated JAR"**  
  5. Click **Finish**  
  6. To launch, run in CLI:  
     ```sh
     .\jre\bin\java.exe --enable-preview -jar bin/racingmaybe.jar
     ```

- **Exporting the final `.jar` (optional, using IntelliJ)**
  1. Create `/bin` folder in the project root
  2. Open `File -> Project Structure` (`Ctrl+Alt+Shift+S`), navigate to `Artifacts` and click the `+` button
  2. Select `JAR` -> `From modules with dependencies`
  3. Select main class as `Main.main`
  4. Directory of manifest should be the `/res` folder in the project
  3. Set **Output Directory** in Artifacts menu inside the created `bin/` folder
  4. Click **Apply** and **OK**
  5. Build the artifact by selecting `Build -> Build Artifacts...` and selecting the artifact
  6. To launch, right-click created `RM.jar` and select `Run`

# Basic code structure
```
UIUsernameModal                ┌─────────────────────────────────────────────────┐
  ▲     │                      │                                                 │
  │     ▼                      ▼                                                 │
  │   Lobby/Race ◀────────► GameInfo ──► Message ──► Remote ──► Message ──► Translator
  │     ▲                      │
  │     │                      ├── GameMode
SceneHandler ◀─ InputHandler   │   • Points Management
     ▲                         │   • Money Management
     │                         │   • Track Length
   Main                        │   • Rounds Management
                               │
                               ├── Players
                               │   └── Player
                               │       ├── History
                               │       ├── Bank
Lobby                          │       ├── Upgrades
├── CarChoiceSubscene          │       ├── Layer
└── UpgradesSubscene           │       └── Car
                               │
Race                           ├── Store 
├── RaceVisual                 │ 
├── FinishVisual               └── GameRemoteMaster
└── WinVisual                      • Game End Control
                                                                
Car
├── CarFuncs
├── CarStats
├── CarAudio
├── CarModel
└── Rep

Upgrades & Layer & TileVisual (+TilePiece)
├── Upgrade
│   ├── RegVals
│   └── UIBonusModal
├── Tool
└── EmptyTile 
 
Features
└── Global Interaction Access
```
