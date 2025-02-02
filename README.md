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
- The `res/` folder from inside `bin/racingmaybe.jar`  
- The following folders:  
  - `audio`  
  - `images`  
  - `models`  
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

##  Contribution & Redistribution  

- **Redistribution is not allowed**, but you are **free to modify** the game and contribute!  
- If you submit a **pull request**, it is likely to be included in the next patch! ^^

