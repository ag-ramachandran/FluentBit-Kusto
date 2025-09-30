using System.Globalization;
using System.Text.Json;
using System.Text.Json.Serialization;

var cts = new CancellationTokenSource();
Console.CancelKeyPress += (_, args) =>
{
    Console.WriteLine("Stopping .NET log producer...");
    cts.Cancel();
    args.Cancel = true;
};

var logPath = Environment.GetEnvironmentVariable("DOTNET_LOG_PATH") ?? "/logs/dotnet-app.log";
var logIntervalSeconds = double.TryParse(
        Environment.GetEnvironmentVariable("LOG_INTERVAL_SECONDS"),
        NumberStyles.Float,
        CultureInfo.InvariantCulture,
        out var parsedInterval)
    ? Math.Max(parsedInterval, 0.1)
    : 0.1;
var appName = Environment.GetEnvironmentVariable("APP_NAME") ?? "dotnet-log-producer";

var directory = Path.GetDirectoryName(logPath);
if (!string.IsNullOrEmpty(directory))
{
    Directory.CreateDirectory(directory);
}

Console.WriteLine($"Starting .NET log producer. Writing to {logPath} every {logIntervalSeconds:F3} seconds.");

await using var stream = new FileStream(logPath, FileMode.Append, FileAccess.Write, FileShare.ReadWrite);
await using var writer = new StreamWriter(stream) { AutoFlush = true };
var jsonOptions = new JsonSerializerOptions
{
    DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull,
    PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
    WriteIndented = false
};

var random = new Random();
var levels = new[] { "INFO", "DEBUG", "WARN", "ERROR" };

while (!cts.IsCancellationRequested)
{
    var logEntry = new
    {
        timestamp = DateTimeOffset.UtcNow,
        level = levels[random.Next(levels.Length)],
        app = appName,
        correlationId = Guid.NewGuid(),
        message = "Processed work item",
        durationMs = random.Next(5, 750),
        userId = random.Next(1000, 1100),
        tags = new[] { "example", "dotnet" }
    };

    var json = JsonSerializer.Serialize(logEntry, jsonOptions);
    await writer.WriteLineAsync(json);
    Console.WriteLine(json);

    try
    {
    await Task.Delay(TimeSpan.FromSeconds(logIntervalSeconds), cts.Token);
    }
    catch (TaskCanceledException)
    {
        break;
    }
}

Console.WriteLine(".NET log producer stopped.");
