#!/bin/bash

N=$1

for ((i=1;i<=N;i++))
do
    clj -A:single-process script.clj 1 1 &
done

wait
echo "all done"
