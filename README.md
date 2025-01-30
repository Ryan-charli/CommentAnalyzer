MIT License


# Code Comment Analyzer
A tool for analyzing code comments and code quality in Java projects.

## Updates:
**Optimized Comment Analysis**
- Removed code analysis functionality
- Improved multi-line comment handling logic
- Reduced memory usage
**New Features**
- Added multi-threading support to improve analysis efficiency
- Implemented progress display for better visualization
- Added summary information display, including:
* Total number of files 
* Total number of comments
* Average quality score
## Features
- Comment extraction and analysis
- Code complexity metrics
- Comment quality scoring
- Code smell detection
- Export analysis reports

## Requirements
- Java 23
- Maven 3.9.9

## Installation
```bash
git clone [repository-url]
cd commentanalyzer
mvn clean install
```

## Usage
1. Run `CommentAnalyzerApp`
2. Click "Select Directory" to choose Java project
3. View analysis results
4. Export report using "Export Report" button

## Metrics
- Comment density
- Code complexity
- Documentation completeness
- Code smells

## Project Structure
```
src/main/java/
├── ai/
├── analysis/
├── parser/
└── ui/
```
## Code Comment Analysis Standard
**A code comment quality analyzer based on academic research:**
 * 1. Steidl et al. (2013) "Quality Analysis of Source Code Comments" - ICPC 2013
 * 2. Khamis et al. (2010) "Automatic Quality Assessment of Source Code Comments" - NLDB 2010
 * 3. Padioleau et al. (2009) "Documenting and Automating Collateral Evolution in Linux Device Drivers" - EuroSys 2009