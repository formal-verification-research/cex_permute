---
title: "File Types"
author: "Landon Taylor"
created: "08 June 2022"
---

# File Types

There are a number of file extensions and specific file names involved in this project. Here is what each means:

## File Extensions

Usually, the file extension will be a good indicator of what a file does. This project uses some standard and some custom file extensions.

- `.py` is a python file. These are the main script files.
- `.java` is a Java file. These interact with the PRISM API.
- `.txt` is a plain-text file, usually a report for the user.
- `.trace` is a machine-readable trace, usually showing a single path.
- `.ivy` is an IVy model file.
- `.sm` is a PRISM model file.
- `.csl` holds a property for the model (usually a CSL property).
- `.sta` is a prism-readable list of states (see [PRISM Manual](http://prismmodelchecker.org/manual/Appendices/ExplicitModelFiles)).
- `.tra` is a prism-readable list of transitions (see [PRISM Manual](http://prismmodelchecker.org/manual/Appendices/ExplicitModelFiles)).
- `.lab` is a prism-readable list of state labels (see [PRISM Manual](http://prismmodelchecker.org/manual/Appendices/ExplicitModelFiles)).
- `.result` is a machine-readable intermediate output file.
- `.md` is a Markdown documentation file.
- `.tex` is a LaTeX documentation file.

## Specific Files

There are a number of highly important files in this project. Here are the most relevant:

- `Makefile` coordinates the compilation and execution of the PRISM API.

- `main.py` is the primary script. 
  Start the whole process by calling `python main.py` or `python3 main.py`.

- `model.ivy` is the original (user-generated) IVy model.

- `model.sm` is the original (user-generated) PRISM model.

- `model.sta,tra,lab` are machine-generated state matrix files. For best results, do not edit these files (see [PRISM Manual](http://prismmodelchecker.org/manual/Appendices/ExplicitModelFiles)).

- `final_prism_result.txt` stores the final result from model checking in PRISM. This is where you will find the probability of reaching your property.

- `src/SimulateModel.java` is the Java file responsible for interacting with PRISM to simulate and build the model.