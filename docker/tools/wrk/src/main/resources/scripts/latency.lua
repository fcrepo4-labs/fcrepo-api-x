-- example reporting script which demonstrates a custom
-- done() function that prints latency percentiles as CSV

done = function(summary, latency, requests)
   io.write("\nLatency(μs)\n------------------------------\n")
   for _, p in pairs({ 50, 90, 95, 97.5, 99, 99.9}) do
      n = latency:percentile(p)
      io.write(string.format("%g%%\t%d\n", p, n))
   end
end
