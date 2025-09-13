# Pre-deployment Checker Guide

## Overview

The Pre-deployment Checker is a comprehensive validation tool that ensures your application is ready for deployment to AWS Amplify. It performs thorough checks on configuration files, dependencies, build commands, and required files before deployment.

## Features

### 1. amplify.yml Syntax Validation
- ✅ YAML syntax validation
- ✅ Structure validation (version, frontend, backend sections)
- ✅ File reference validation
- ✅ Build command syntax checking
- ✅ Windows and Unix compatibility

### 2. Required Files Existence Check
- ✅ Frontend files validation
- ✅ Backend files validation
- ✅ Configuration files validation
- ✅ Directory structure validation

### 3. Build Commands Executability Validation
- ✅ Basic system commands testing
- ✅ Frontend build commands validation
- ✅ Backend build commands validation
- ✅ Cross-platform command compatibility

### 4. Dependencies Availability Verification
- ✅ System dependencies (Node.js, npm, Git)
- ✅ Frontend dependencies (package.json, node_modules)
- ✅ Backend dependencies (Java, Maven, pom.xml)
- ✅ Version compatibility checking

## Usage

### Command Line Usage

#### Basic Usage
```bash
node pre-deployment-checker.js
```

#### Verbose Mode
```bash
node pre-deployment-checker.js --verbose
```

#### Skip Specific Checks
```bash
# Skip dependency checks
node pre-deployment-checker.js --skip-deps

# Skip command checks
node pre-deployment-checker.js --skip-commands

# Skip both
node pre-deployment-checker.js --skip-deps --skip-commands
```

### Batch Scripts

#### Windows Batch File
```batch
run-pre-deployment-check.bat
```

#### PowerShell Script
```powershell
.\run-pre-deployment-check.ps1
```

### Programmatic Usage

```javascript
const PreDeploymentChecker = require('./pre-deployment-checker');

const checker = new PreDeploymentChecker({
    verbose: true,
    skipDependencyCheck: false,
    skipCommandCheck: false
});

checker.runPreDeploymentChecks()
    .then(success => {
        if (success) {
            console.log('Ready for deployment!');
        } else {
            console.log('Issues found, please fix before deploying.');
        }
    })
    .catch(error => {
        console.error('Check failed:', error);
    });
```

## Validation Details

### amplify.yml Validation

The tool validates the following aspects of your `amplify.yml` file:

#### Required Sections
- `version`: Must be specified
- `frontend` or `backend`: At least one must be present

#### Frontend Configuration
- `phases`: Build phases (preBuild, build, postBuild)
- `artifacts`: Output artifacts specification
- `cache`: Caching configuration (optional)

#### Backend Configuration
- `phases`: Build phases with Maven commands
- `artifacts`: JAR file output specification
- `cache`: Maven repository caching (optional)

#### Common Issues Detected
- Missing required sections
- Invalid YAML syntax
- Inconsistent indentation
- Missing file references
- Unsafe command patterns

### File Validation

#### Required Files
- `amplify.yml` - Amplify configuration
- `frontend/index.html` - Main HTML file
- `frontend/css/style.css` - Stylesheet
- `frontend/js/app.js` - Main JavaScript
- `frontend/js/api-client.js` - API client
- `backend/pom.xml` - Maven configuration
- `backend/src/main/java/` - Java source directory

#### Optional Files
- `frontend/package.json` - Frontend dependencies
- `template.yaml` - SAM template
- `samconfig.toml` - SAM configuration
- `backend/src/main/resources/application.yml` - Spring configuration

### Command Validation

#### Basic Commands
- `echo` - Basic output command
- `dir` (Windows) / `ls` (Unix) - Directory listing
- `if exist` (Windows) / `test` (Unix) - File existence check

#### Frontend Commands
- File existence validation
- Directory listing capability
- Static file processing commands

#### Backend Commands
- Java availability and version
- Maven or Maven wrapper availability
- pom.xml validation
- Maven configuration testing

### Dependency Validation

#### System Dependencies
- **Node.js**: JavaScript runtime (optional)
- **npm**: Package manager (optional)
- **Git**: Version control (required)

#### Frontend Dependencies
- package.json structure validation
- Build script availability
- node_modules directory check

#### Backend Dependencies
- Java version compatibility (17+ recommended)
- Maven wrapper availability
- Essential Spring Boot dependencies
- AWS Lambda dependencies

## Output and Reports

### Console Output

The tool provides real-time feedback with color-coded messages:
- ✅ **Success**: Green checkmarks for passed validations
- ⚠️ **Warning**: Yellow warnings for non-critical issues
- ❌ **Error**: Red X marks for critical issues that must be fixed
- ℹ️ **Info**: Blue information messages for context

### JSON Report

A detailed JSON report is generated at `pre-deployment-check-report.json`:

```json
{
  "timestamp": "2024-01-15T10:30:00.000Z",
  "summary": {
    "successes": 45,
    "warnings": 4,
    "errors": 0,
    "criticalErrors": 0,
    "deploymentReady": true
  },
  "successes": [...],
  "warnings": [...],
  "errors": [...],
  "checkedItems": [...]
}
```

### Deployment Readiness Assessment

The tool provides a clear assessment:
- ✅ **Ready for deployment**: No errors found
- ⚠️ **Deployment possible but errors should be fixed**: Non-critical errors present
- ❌ **Not ready - critical issues must be resolved**: Critical errors must be fixed

## Common Issues and Solutions

### amplify.yml Issues

#### Issue: "Missing version field"
```yaml
# Add version at the top of amplify.yml
version: 1
```

#### Issue: "Backend build phase has no commands"
```yaml
backend:
  phases:
    build:
      commands:
        - cd backend
        - mvn clean package -DskipTests
```

#### Issue: "Referenced directory not found"
```yaml
# Ensure the baseDirectory exists
frontend:
  artifacts:
    baseDirectory: frontend  # This directory must exist
```

### File Issues

#### Issue: "Required file missing"
- Ensure all required files exist in the correct locations
- Check file paths and naming conventions
- Verify directory structure matches expectations

### Command Issues

#### Issue: "Required command not available"
- Install missing system dependencies
- Ensure commands are in system PATH
- Use platform-appropriate commands

### Dependency Issues

#### Issue: "Java version below recommended"
```xml
<!-- Update pom.xml -->
<properties>
    <java.version>17</java.version>
</properties>
```

#### Issue: "Maven wrapper not found"
```bash
# Generate Maven wrapper
cd backend
mvn wrapper:wrapper
```

## Integration with CI/CD

### GitHub Actions

```yaml
name: Pre-deployment Check
on: [push, pull_request]

jobs:
  pre-deployment-check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - uses: actions/setup-node@v2
        with:
          node-version: '16'
      - name: Run pre-deployment checks
        run: node pre-deployment-checker.js
```

### AWS CodeBuild

```yaml
version: 0.2
phases:
  pre_build:
    commands:
      - echo "Running pre-deployment checks"
      - node pre-deployment-checker.js
  build:
    commands:
      - echo "Pre-deployment checks passed, proceeding with build"
```

## Testing

### Running Tests

```bash
# Run the test suite
node test-pre-deployment-checker.js
```

### Test Coverage

The test suite validates:
- amplify.yml syntax validation functionality
- Required files existence checking
- Build commands executability validation
- Dependencies availability verification
- Full integration testing
- Report generation and structure

## Troubleshooting

### Common Error Messages

#### "Command failed: ls --version"
- **Cause**: Running on Windows with Unix commands
- **Solution**: The tool automatically detects platform and uses appropriate commands

#### "Maven configuration validation failed"
- **Cause**: Maven dependencies not downloaded
- **Solution**: Run `mvn dependency:resolve` first, or use `--skip-deps` flag

#### "Frontend node_modules not found"
- **Cause**: npm dependencies not installed
- **Solution**: Run `npm install` in frontend directory

### Debug Mode

Enable verbose logging for detailed troubleshooting:
```bash
node pre-deployment-checker.js --verbose
```

## Requirements Mapping

This implementation satisfies the following requirements:

- **Requirement 1.1**: Detailed error logging and step execution status
- **Requirement 1.2**: Specific configuration problem identification
- **Requirement 3.1**: Valid YAML syntax validation
- **Requirement 3.2**: Executable command validation

## Best Practices

1. **Run before every deployment**: Make pre-deployment checks part of your workflow
2. **Fix warnings**: Address warnings to improve deployment reliability
3. **Keep dependencies updated**: Regularly update Java, Maven, and Node.js versions
4. **Use version control**: Commit configuration changes before deployment
5. **Monitor reports**: Review generated reports for trends and improvements

## Support

For issues or questions:
1. Check the generated JSON report for detailed error information
2. Run with `--verbose` flag for additional debugging information
3. Review the troubleshooting section above
4. Ensure all system dependencies are properly installed