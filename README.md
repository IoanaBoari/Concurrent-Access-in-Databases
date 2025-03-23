// BOARI IOANA-RUXANDRA 331CD

APD - Homework 1b - Concurrent Access in Databases

Local test: 18/18 passed

  To complete this assignment, I started by creating my own ThreadPool, which takes the number of threads
as an argument and initializes them when the pool is created. The threads take tasks from a queue
and execute them accordingly. To ensure proper shutdown and thread safety, I implemented the shutdown()
and awaitTermination() methods, using a special task ("Poison Pill") to signal the threads to stop.
This design ensures that all tasks are correctly processed before the thread pool shuts down.

  In the first solution, ReaderPreferred, I chose to use a semaphore to control database access and a
ReentrantLock to maintain the integrity of the reader count. Readers have priority and can access
the resource concurrently as long as there are no active writers. Writers wait until all readers finish. 
The advantage of this solution is high performance when there are many read operations. 
The disadvantage is the potential starvation of writers.

  In the second solution, WriterPreferred1, I used semaphores and ReentrantLock to manage database access
and safely update the number of readers and writers. Writers have priority, and readers wait if there are writers.
Readers access the resource only when there are no writers waiting. The advantage is reduced latency 
for updating data. The disadvantage is that readers might experience delays when there are many write operations.

  In the third solution, WriterPreferred2, I used Java monitors (synchronized, .wait(), .notifyAll())
for synchronization. Writers have absolute priority and access the resource exclusively when there are no readers.
Readers wait if there are writers. The advantage is fast data updates. The disadvantage is that readers 
can experience starvation, and the implementation is more complex.

  Both methods for writer's priority were tested using the same set of tests, and after multiple runs, 
I did not observe significant performance differences. The execution time is mostly the same, 
with differences of at most one second. Sometimes the difference is for WriterPreffered1 and 
sometimes for WriterPreffered2 so I can not distinguish major differences in performance.

  Regarding the tests, I noticed from the start that tests with a higher number of tasks take longer to complete 
and have a higher timeout. Longer read and write durations increase the total execution time because threads 
need to wait longer to finish their operations. Analyzing the tests for ReaderPreferred, I observed that 
even though a larger number of threads (e.g., 12 in test ReaderPreferred2) might seem like it would speed up execution, 
it actually slows down performance when the available resources, such as a small storageSize, are limited. 
Too many threads competing for the same resources lead to longer synchronization times.

  In the tests, the "readersToWritersRatio" is smaller for ReaderPreferred tests compared to WriterPreferred tests. 
This means there are more writeer tasks than reader tasks in the ReaderPreferred tests. This setup allows 
for evaluating the capacity of this strategy to maintain reader priority even when writers dominate.

  Analyzing the tests for the two WriterPreferred solutions, I observed the same behavior regarding 
the high number of threads and the small database size: tests take longer in these conditions 
compared to having fewer threads and a larger database size. When there are many threads (12),
each thread tries to access database resources. With a small database size, threads compete intensively 
for the same storage locations. This competition increases wait times because writers need exclusive access 
to resources, which blocks readers and other writers. Readers must wait for writers to finish before accessing the resource.

   From analyzing the tests, I noticed that the best performance is achieved when the number of threads is lower 
(8 threads) and the database size is larger (testValues.length * 2) because excessive competition for resources is reduced.

   This assignment demonstrated the importance of efficient synchronization and proper management of concurrent access to databases.
The implemented solutions (ReaderPreferred, WriterPreferred1, and WriterPreferred2) proved to be functional 
and efficient in the tests. Choosing the most suitable solution depends on the application's requirements, 
where the balance between the number of readers and writers, the database size, and the thread configuration must be considered.
