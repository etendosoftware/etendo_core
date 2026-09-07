# Platform HTTP compatibility runtime

Boot the real DAL and expose the existing Product datasource implementation at
the Classic-compatible path. Configuration is external and database connections
are read-only. No Classic web listeners or background jobs are registered here.
