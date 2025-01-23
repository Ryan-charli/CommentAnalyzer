MIT License

# Code Comment Analyzer
A tool for analyzing code comments and code quality in Java projects.

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