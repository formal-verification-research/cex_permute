# Input File Format

Because of the extraordinary number of possible input parameters and 
the high desirability to retain parameters, the input is a file
called `commute_input.txt` rather than command-line arguments.

The file works somewhat similarly to command-line arguments.
Before executing `make test`, update the file `commute_input.txt`
in the working directory.

The file is of the following format:

```
option parameter
option parameter
option parameter
```

The available options and their parameters are as follows. Note that if 
neither `timeBound` nor `recursionBound` is selected, the tool will 
default to a recursion bound of `5` to guarantee termination.

- `model` (required)
    - Input the path to the input PRISM model file (e.g. `model.sm`)

- `property` (required)
    - Input the desired property without time bounds (e.g. `A = 20`)

- `timeBound` (recommended)
    - Input the desired time bound, presumably in seconds (e.g. `200`) to provide a heuristic for termination

- `recursionBound` (recommended)
    - Input the desired bound on recursion depth (e.g. `30`) to guarantee termination

- `export`
    - Default option `both` exports both a prism and storm-readable model in explicit format
    - Option `storm` exports only a storm-readable model in explicit format
    - Option `prism` exports only a prism-readable model in explicit format
