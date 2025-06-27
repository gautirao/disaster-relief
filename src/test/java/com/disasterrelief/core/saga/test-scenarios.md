SagaManager Test Scenarios
|
+-- Successful Scenarios
|    |
|    +-- Saga Starts and Completes Successfully
|    |     - Given a CommandIssuedEvent, saga starts
|    |     - When first member acknowledges, saga remains pending
|    |     - When all expected members acknowledge, saga completes and is removed
|    |
|    +-- Saga Replays Events
|          - Given a sequence of past events (CommandIssuedEvent + acknowledgments)
|          - When events are replayed
|          - Then saga reconstructs state and completes
|          - Then saga is removed from active sagas
|
+-- Failure Scenarios
|
+-- Ignores Events for Other Commands
|     - Given an event with an unrelated commandId
|     - When handled by SagaManager
|     - Then no saga is created or updated
|     - Then active saga list remains empty
|
+-- Duplicate Acknowledgements
|     - Given a member acknowledges multiple times
|     - When saga handles duplicate acknowledgments
|     - Then it does not affect saga status or acknowledged list redundantly
|
+-- Missing Expected Acknowledger
|     - Given one or more expected acknowledgers never acknowledge
|     - When events processed within deadline (if any)
|     - Then saga remains in pending or fails due to timeout (if timeout logic implemented)
|
+-- Null or Invalid Events
|     - Given null or malformed events
|     - When handled by SagaManager
|     - Then events are ignored or cause a controlled error, not crash
|
+-- Partial Acknowledgement Beyond Deadline
|     - Given some acknowledgements arrive after deadline (future extension)
|     - When saga handles late acknowledgements
|     - Then either ignore or compensate as per timeout/failure compensation logic
|
+-- Empty Expected Acknowledger Set
|     - Given CommandIssuedEvent with empty expected acknowledgers set
|     - When saga is created
|     - Then saga initialization fails or immediately completes
|
+-- Replaying Events with Missing Intermediate Events
|     - Given replay of incomplete event stream missing critical events (e.g. CommandIssuedEvent)
|     - When saga tries to rehydrate
|     - Then saga fails to reconstruct or stays incomplete
