import json
import psutil
from opentelemetry import trace, metrics
from opentelemetry.sdk.resources import Resource
from opentelemetry.semconv.resource import ResourceAttributes
from opentelemetry.sdk.trace import TracerProvider, sampling
from opentelemetry.sdk.trace.export import BatchSpanProcessor

# Import exporters
from opentelemetry.exporter.otlp.proto.http.trace_exporter import OTLPSpanExporter
from opentelemetry.exporter.otlp.proto.http.metric_exporter import OTLPMetricExporter
from opentelemetry.exporter.otlp.proto.http._log_exporter import OTLPLogExporter

#DT_API_URL = ""
#DT_API_TOKEN = ""

"""A class for controlling custom OpenTelemetry behavior"""
class CustomOpenTelemetry():
    def __init__(self):
        self.setup_exporters()
        self.tracer = trace.get_tracer("perform-hot")

    """Sets up OpenTelemetry Trace & Metrics export"""
    def setup_exporters(self):
        # Basic resource details
        merged = dict()
        for name in ["dt_metadata_e617c525669e072eebe3d0f08212e8f2.json", "/var/lib/dynatrace/enrichment/dt_metadata.json", "/var/lib/dynatrace/enrichment/dt_host_metadata.json"]:
            try:
                data = ''
                with open(name) as f:
                    data = json.load(f if name.startswith("/var") else open(f.read()))
                    merged.update(data)
            except:
                pass

        merged.update({
        "service.name": "pysrvc svc on port 8090",
        "service.version": "v1.0.0",
        })
        resource = Resource.create(merged)

        # Set up trace export
        tracer_provider = TracerProvider(
            sampler=sampling.ALWAYS_ON,
            resource=resource
        )
        tracer_provider.add_span_processor(
            BatchSpanProcessor(OTLPSpanExporter(
                endpoint="http://localhost:14499/otlp/v1/traces"
                # endpoint = DT_API_URL + "/v1/traces",
                # headers = {"Authorization": "Api-Token " + DT_API_TOKEN}
            ))
        )
        trace.set_tracer_provider(tracer_provider)

# Sets everything up and can be reused anywhere in the code
ot = CustomOpenTelemetry()
