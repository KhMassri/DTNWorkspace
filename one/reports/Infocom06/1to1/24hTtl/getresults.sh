for bs in 1 10 50 100
do
cat "RapidBS$bs""kTS25k"* | awk ' /delivery_prob:/ { D=$2 } /overhead_ratio:/ { C=$2 } /latency_avg:/ { L=$2 } /hopcount_avg:/ { H=$2 }  END { print D "\t"C"\t"L"\t"H>> "rapid24hTtl.txt" }'
done


