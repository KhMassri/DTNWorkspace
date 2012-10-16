for bs in 2 5 10 50
do
cat "RapidBS$bs""1kTS256k"* | awk ' /delivery_prob:/ { D=$2 } /overhead_ratio:/ { C=$2 } /latency_avg:/ { L=$2 }  END { print D "\t"C"\t"L>> "rapid.txt" }'
done
