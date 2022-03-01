# Commuting Transitions to Discover Additional Paths

## Prerequisites
- Install ivy1.7 with submodules
- Install Prism4.6
- Install jdk13
- Install Python 2.7 (for IVy)
- Install Python 3 (for script, tested on Python 3.9)

## Running the Script
If the prerequisites are installed correctly (meaning `ivy_check`, `python2.7`, `python3`, `java` and `javac` all work properly), the following command will execute the entire script and provide you with interactive instructions when needed.
```
$ python main.py
```

If you wish to use command-line arguments rather than using the prompts, use the following command-line argument structure:
```
$ python main.py ivy_model.ivy prism_model.sm
```

## File Descriptions:
- `main.py` is the main script. Not much happens here on a technical level, but the script orchestrates the rest of the actions.
- `ivy.py` interacts with the IVy model checker.
- `prism-api.py` interacts with the PRISM model checker API.
- `commute.py` handles the actual commuting algorithm.

## Model Assumptions
- Model does not include constants without setting their values
- 