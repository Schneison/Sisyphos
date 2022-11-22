# Sisyphos
> Winning entry for a programming competition at a local university in the module algorithms and data structures
<image src="https://repository-images.githubusercontent.com/508426623/42d8425e-3110-4a38-9639-ad60ee726a31" height="200">


## Exercise
The aim of the competition was to find the fastest route between different points ("materials") and an entry point ("factory") on an NxN grid. 
There were materials, fields on which several materials were placed. The only restrictions that existed were that you could only transport 3
materials at a time until you had to go back to the factory and each field had a certain time requirement that varied from field to field.
There were a total of 2*N materials on the field. The factory was at the bottom, in the middle of the field.

## Goal
The path between the materials that requires the fewest time units.

## Main Problems
- Path finding
- Fast way to all nodes in minmal time


## Approach
My main approach was to try to cluster all matrial in packages of 3 based and then optimise these packjges until we get not the best but a very good result.
For the path finding I used the an own implementation of the [Dijkstra Algorithm](https://en.wikipedia.org/wiki/Dijkstra%27s_algorithm). For the opttimisation factor
of this algorithem i used the distance between the different nodes.

## Package Optimising
For the optimising of the node packages I used an simple generic alogorithem, which resultet in a fast way to optimise the nodes in the packages by
there distance to other nodes and the factory.

---
## License
Copyright (c) 2022 Larson Schneider<br>
Licensed under the MIT license.
