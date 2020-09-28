apply(plugin = "jcstress")

jcstress {
    verbose = true
    timeMillis = "200"
    forks = 0
    iterations = 1
}
