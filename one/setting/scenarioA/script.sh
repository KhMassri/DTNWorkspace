
for r in RandomWaypoint RandomWalk LevyWalk
do
for n in 10 30 70 100
do


cat "reportOfSprayAndWaitRouter$r""nrOfHosts$n""Seed"* | awk ' /delivery_prob:/ { D+=$2 } /overhead_ratio:/ { O+=$2 } /latency_avg:/ { T+=$2 } /hopcount_avg/ { H+=$2 } END { print "DeliveryRatio: "(D+0)/5 "\t Overhead: "(O+0)/5 "\t Delay: "(T+0)/5 "\t HopCount: "(H+0)/5 >> "result.txt" }'
done
done

