using System.Text;
using GeoAppAPI.Models;
using InfluxDB.Client;
using InfluxDB.Client.Api.Domain;
using InfluxDB.Client.Linq;

namespace GeoAppAPI.Services;

public class InfluxService(InfluxDBClient influxDbClient)
{
    // Flux documentation: https://docs.influxdata.com/influxdb/cloud/query-data/get-started/query-influxdb/
    
    public async Task<List<Data>> QueryAsync(string userId, 
        DateTimeOffset? from, 
        DateTimeOffset? to, 
        CancellationToken cancellationToken)
    {
        var bucketName = _getBucketName(userId);
        await _ensureBucketExistsAsync(bucketName, cancellationToken).ConfigureAwait(false);
        
        var api = influxDbClient.GetQueryApi();

        var flux = _buildFluxQuery(bucketName, from, to);
        var results = await api
            .QueryAsync<Data>(flux, Organisation, cancellationToken)
            .ConfigureAwait(false);
        
        return results;
    }

    public async Task WriteAsync(string userId, IEnumerable<Data> data, CancellationToken cancellationToken)
    {
        var bucketName = _getBucketName(userId);
        await _ensureBucketExistsAsync(bucketName, cancellationToken).ConfigureAwait(false);
        
        var writeApi = influxDbClient.GetWriteApiAsync();

        await writeApi
            .WriteMeasurementsAsync<Data>(data.ToList(), bucket: bucketName, org: Organisation, cancellationToken: cancellationToken)
            .ConfigureAwait(false);
    }
    
    private async Task _ensureBucketExistsAsync(string bucketName, CancellationToken cancellationToken)
    {
        var orgId = await _ensureOrganisationExistsAsync(cancellationToken).ConfigureAwait(false);
        
        var bucketsApi = influxDbClient.GetBucketsApi();

        var bucket = await bucketsApi
            .FindBucketByNameAsync(bucketName, cancellationToken)
            .ConfigureAwait(false);

        if (bucket == null)
        {
            var retentionRule = new BucketRetentionRules(everySeconds: 0);
            await bucketsApi
                .CreateBucketAsync(bucketName, retentionRule, orgId, cancellationToken)
                .ConfigureAwait(false);
        }
    }
    
    private async Task<string> _ensureOrganisationExistsAsync(CancellationToken cancellationToken)
    {
        var organisationsApi = influxDbClient.GetOrganizationsApi();
        
        var organisations = await organisationsApi
            .FindOrganizationsAsync(cancellationToken: cancellationToken)
            .ConfigureAwait(false);

        var organisation = organisations
            .FirstOrDefault(org => org.Name == Organisation); 
            
        organisation ??= await organisationsApi
            .CreateOrganizationAsync(Organisation, cancellationToken)
            .ConfigureAwait(false);

        return organisation.Id;
    }

    private static string _getBucketName(string userId)
    {
        return $"user-{userId}";
    }

    private static string _buildFluxQuery(string bucketName, DateTimeOffset? from, DateTimeOffset? to)
    {
        var fluxBuilder = new StringBuilder("from(bucket: \"");
        fluxBuilder.Append(bucketName);
        fluxBuilder.Append("\")");

        if (!from.HasValue && !to.HasValue)
        {
            return fluxBuilder.ToString();
        }
        
        fluxBuilder.Append(" |> range(");

        if (from.HasValue && to.HasValue)
        {
            fluxBuilder.Append("start: ");
            fluxBuilder.Append(from.Value.ToString("s"));
            fluxBuilder.Append("Z, stop: ");
            fluxBuilder.Append(to.Value.ToString("s"));
            fluxBuilder.Append('Z');
        }
        else if (from.HasValue)
        {
            fluxBuilder.Append("start: ");
            fluxBuilder.Append(from.Value.ToString("s"));
            fluxBuilder.Append('Z');
        }
        else if (to.HasValue)
        {
            fluxBuilder.Append("start: 1970-01-01T00:00:00Z, stop: ");
            fluxBuilder.Append(to.Value.ToString("s"));
            fluxBuilder.Append('Z');
        }
            
        fluxBuilder.Append(')');

        return fluxBuilder.ToString();
    }
    
    private const string Organisation = "GeoApp";
}