import kagglehub
import inspect
print("kagglehub file:", kagglehub.__file__)
try:
    import kagglehub.config
    print("kagglehub.config file:", inspect.getfile(kagglehub.config))
    # Print the source code of get_cache_folder
    import inspect
    print(inspect.getsource(kagglehub.config.get_cache_folder))
except Exception as e:
    print("Error:", e)
