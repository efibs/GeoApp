using InfluxDB.Client;

var builder = WebApplication.CreateBuilder(args);

// Add services to the container.

builder.Services.AddControllers();
// Learn more about configuring Swagger/OpenAPI at https://aka.ms/aspnetcore/swashbuckle
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

var influxDbHost = builder.Configuration.GetValue<string>("InfluxDB:Host")!;
var influxDbToken = builder.Configuration.GetValue<string>("InfluxDB:Token")!;
builder.Services.AddTransient(_ => new InfluxDBClient(influxDbHost, influxDbToken));

var app = builder.Build();

// Configure the HTTP request pipeline.
if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.UseHttpsRedirection();

app.UseAuthorization();

app.MapControllers();

app.Run();