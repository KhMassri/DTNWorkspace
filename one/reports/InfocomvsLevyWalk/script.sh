for b in 10k 100k
do
for n in 120,121 240,241 360,361 600,601
do
cat "FuzzySprayBS$b""MT$n"* | awk ' /delivery_prob:/ { D=$2 } /overhead_ratio:/ { O=$2 } /latency_avg:/ { T=$2 } /hopcount_avg/ { H=$2 } END { print (D+0) "\t"(O+0) "\t"(T+0) "\t"(H+0) >> "fuzzyspray.dat" }'
done
done
